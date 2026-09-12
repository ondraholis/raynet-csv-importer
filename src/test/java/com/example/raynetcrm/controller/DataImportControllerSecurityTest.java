package com.example.raynetcrm.controller;

import com.example.raynetcrm.configuration.security.ApiKeyProperties;
import com.example.raynetcrm.configuration.security.SecurityConfig;
import com.example.raynetcrm.service.ClientImporterCSV;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataImportController.class)
@Import({SecurityConfig.class, ApiKeyProperties.class})
@TestPropertySource(properties = "security.api-key=" + DataImportControllerSecurityTest.VALID_API_KEY)
class DataImportControllerSecurityTest {

    static final String VALID_API_KEY = "test-secret-key";
    private static final String API_KEY_HEADER = "X-API-Key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientImporterCSV clientImporterCSV;

    @Test
    void uploadData_withValidApiKey_isAccepted() throws Exception {
        mockMvc.perform(csvUpload().header(API_KEY_HEADER, VALID_API_KEY))
                .andExpect(status().isAccepted());

        verify(clientImporterCSV).processCsv(any());
    }

    @Test
    void uploadData_withoutApiKey_isUnauthorizedAndDoesNotImport() throws Exception {
        mockMvc.perform(csvUpload())
                .andExpect(status().isUnauthorized());

        verify(clientImporterCSV, never()).processCsv(any());
    }

    @Test
    void uploadData_withWrongApiKey_isUnauthorizedAndDoesNotImport() throws Exception {
        mockMvc.perform(csvUpload().header(API_KEY_HEADER, "wrong-key"))
                .andExpect(status().isUnauthorized());

        verify(clientImporterCSV, never()).processCsv(any());
    }

    private static MockMultipartHttpServletRequestBuilder csvUpload() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "clients.csv", "text/csv", "regNumber;title;email;phone\n".getBytes());
        return multipart("/uploadData").file(file);
    }
}