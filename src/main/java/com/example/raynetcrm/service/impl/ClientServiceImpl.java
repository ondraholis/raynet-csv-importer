package com.example.raynetcrm.service.impl;

import com.example.raynetcrm.dto.ClientCsvBean;
import com.example.raynetcrm.entity.Client;
import com.example.raynetcrm.repository.ClientRepository;
import com.example.raynetcrm.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {
    private final ClientRepository clientRepository;
    @Override
    public boolean clientExists(String regNumber) {
        return clientRepository.existsByRegNumber(regNumber);
    }

    @Override
    public Client createClient(ClientCsvBean clientCsvBean) {
        Client client = new Client();
        client.setRegNumber(clientCsvBean.getRegNumber());
        client.setTitle(clientCsvBean.getTitle());
        client.setEmail(clientCsvBean.getEmail());
        client.setPhone(clientCsvBean.getPhone());
        return clientRepository.save(client);
    }

    @Override
    public List<Client> updateClient(ClientCsvBean clientCsvBean) {
        List<Client> clientsToUpdate = clientRepository.findAllByRegNumber(clientCsvBean.getRegNumber());
        List<Client> updatedClients = new ArrayList<>();

        for (Client client : clientsToUpdate) {
            client.setTitle(clientCsvBean.getTitle());
            client.setEmail(clientCsvBean.getEmail());
            client.setPhone(clientCsvBean.getPhone());
            updatedClients.add(clientRepository.save(client));
        }
        return updatedClients;
    }

    @Override
    public List<Client> findAllReadyForUpsert() {
        return clientRepository.findAllByUpsertDateIsNull();
    }

    @Override
    public Client markClientAsUpserted(Client client){
        client.setUpsertDate(Instant.now());
        return clientRepository.save(client);
    }
}
