package com.example.raynetcrm.service.impl;

import com.example.raynetcrm.dto.ClientCsvBean;
import com.example.raynetcrm.entity.Client;
import com.example.raynetcrm.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ClientServiceImplTest {

    private static final String REG_NUMBER = "123456789";
    private static final String OLD_TITLE = "Old Title";
    private static final String OLD_EMAIL = "old@example.com";
    private static final String OLD_PHONE = "111-111-1111";
    private static final String NEW_TITLE = "New Title";
    private static final String NEW_EMAIL = "new@example.com";
    private static final String NEW_PHONE = "999-999-9999";

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    @Test
    void testClientExists() {
        // Given
        when(clientRepository.existsByRegNumber(REG_NUMBER)).thenReturn(true);

        // When
        boolean exists = clientService.clientExists(REG_NUMBER);

        // Then
        assertTrue(exists);
    }

    @Test
    void testCreateClient() {
        // Given
        Client expectedClient = createClient(REG_NUMBER, NEW_TITLE, NEW_EMAIL, NEW_PHONE);
        when(clientRepository.save(expectedClient)).thenReturn(expectedClient);

        // When
        Client createdClient = clientService.createClient(
                createClientCsvBean(REG_NUMBER, NEW_TITLE, NEW_EMAIL, NEW_PHONE)
        );

        // Then
        assertNotNull(createdClient);
        assertEquals(expectedClient, createdClient);
    }

    @Test
    void testUpdateClient_AppliesFieldsFromCsvBean() {
        // Given
        Client existingClient = createClient(REG_NUMBER, OLD_TITLE, OLD_EMAIL, OLD_PHONE);

        when(clientRepository.findAllByRegNumber(REG_NUMBER)).thenReturn(List.of(existingClient));
        when(clientRepository.save(existingClient)).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<Client> updatedClients = clientService.updateClient(
                createClientCsvBean(REG_NUMBER, NEW_TITLE, NEW_EMAIL, NEW_PHONE)
        );

        // Then
        Client saved = updatedClients.getFirst();
        assertEquals(NEW_TITLE, saved.getTitle());
        assertEquals(NEW_EMAIL, saved.getEmail());
        assertEquals(NEW_PHONE, saved.getPhone());
    }

    @Test
    void testUpdateClient_UpdatesAllMatchingClients() {
        // Given
        Client client1 = createClient(REG_NUMBER, OLD_TITLE, OLD_EMAIL, OLD_PHONE);
        Client client2 = createClient(REG_NUMBER, OLD_TITLE, OLD_EMAIL, OLD_PHONE);

        when(clientRepository.findAllByRegNumber(REG_NUMBER)).thenReturn(List.of(client1, client2));
        when(clientRepository.save(client1)).thenAnswer(invocation -> invocation.getArgument(0));
        when(clientRepository.save(client2)).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        List<Client> updatedClients = clientService.updateClient(
                createClientCsvBean(REG_NUMBER, NEW_TITLE, NEW_EMAIL, NEW_PHONE)
        );

        // Then
        assertEquals(2, updatedClients.size());
        updatedClients.forEach(c -> {
            assertEquals(NEW_TITLE, c.getTitle());
            assertEquals(NEW_EMAIL, c.getEmail());
            assertEquals(NEW_PHONE, c.getPhone());
        });
    }

    private ClientCsvBean createClientCsvBean(String regNumber, String title, String email, String phone) {
        ClientCsvBean bean = new ClientCsvBean();
        bean.setRegNumber(regNumber);
        bean.setTitle(title);
        bean.setEmail(email);
        bean.setPhone(phone);
        return bean;
    }

    private Client createClient(String regNumber, String title, String email, String phone) {
        Client client = new Client();
        client.setRegNumber(regNumber);
        client.setTitle(title);
        client.setEmail(email);
        client.setPhone(phone);
        return client;
    }
}
