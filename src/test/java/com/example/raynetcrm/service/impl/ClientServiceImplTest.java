package com.example.raynetcrm.service.impl;

import com.example.raynetcrm.dto.ClientCsvBean;
import com.example.raynetcrm.entity.Client;
import com.example.raynetcrm.repository.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ClientServiceImplTest {
    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientServiceImpl clientService;


    @Test
    void testClientExists() {
        // Given
        String regNumber = "123456789";
        when(clientRepository.existsByRegNumber(regNumber)).thenReturn(true);

        // When
        boolean exists = clientService.clientExists(regNumber);

        // Then
        assertTrue(exists);
    }

    @Test
    void testCreateClient() {
        // Given
        Client expectedClient = new Client();
        when(clientRepository.save(any(Client.class))).thenReturn(expectedClient);

        // When
        Client createdClient = clientService.createClient(createClientCsvBean("123456789", "Test Client", "test@example.com", "123-456-7890"));

        // Then
        assertNotNull(createdClient);
        assertEquals(expectedClient, createdClient);
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void testUpdateClient() {
        // Given
        Client expectedClient = new Client();
        List<Client> clientsToUpdate = new ArrayList<>();
        clientsToUpdate.add(expectedClient);

        when(clientRepository.findAllByRegNumber(anyString())).thenReturn(clientsToUpdate);
        when(clientRepository.save(any(Client.class))).thenReturn(expectedClient);

        // When
        List<Client> updatedClients = clientService.updateClient(createClientCsvBean("123456789", "Updated Client", "updated@example.com", "987-654-3210"));

        // Then
        assertFalse(updatedClients.isEmpty());
        assertEquals(expectedClient, updatedClients.getFirst());
        verify(clientRepository).save(any(Client.class));
    }

    private ClientCsvBean createClientCsvBean(String regNumber, String title, String email, String phone) {
        ClientCsvBean bean = new ClientCsvBean();
        bean.setRegNumber(regNumber);
        bean.setTitle(title);
        bean.setEmail(email);
        bean.setPhone(phone);
        return bean;
    }
}
