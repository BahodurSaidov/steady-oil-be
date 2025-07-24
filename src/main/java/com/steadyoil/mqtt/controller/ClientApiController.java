package com.steadyoil.mqtt.controller;

import com.steadyoil.mqtt.domain.Client;
import com.steadyoil.mqtt.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "${com.steadyoil.cors.url}")
@RequestMapping("/api/v1/clients")
@RestController
public class ClientApiController {
    @Autowired
    private ClientService clientService;

    /**
     * Get current client.
     *
     * @return Client
     */
    @GetMapping("/current")
    public ResponseEntity<Client> getCurrentClient() {
        return ResponseEntity.ok().body(clientService.getCurrentClient());
    }


    /**
     * Get sensor ids of given client id.
     *
     * @param id
     * @return List<Long>
     */
    @GetMapping("/{id}/sensors")
    public ResponseEntity<List<Long>> getClientAllSensorsById(@PathVariable long id) {
        return ResponseEntity.ok().body(clientService.getClientById(id).getSensors());
    }

    /**
     * Get all clients
     *
     * @return Page<Client>
     */
    @GetMapping("")
    public ResponseEntity<Page<Client>> getAllClients(@PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC, value = 10) @Valid Pageable page) {
        return ResponseEntity.ok().body(clientService.getAllClient(page));
    }

    /**
     * Get client by id.
     *
     * @param id
     * @return Client
     */
    @GetMapping("/{id}")
    public ResponseEntity<Client> getClientById(@PathVariable long id) {
        return ResponseEntity.ok().body(clientService.getClientById(id));
    }

    /**
     * Save new client.
     *
     * @param client
     * @return Client
     */
    @PostMapping("")
    public ResponseEntity<Client> createClient(@RequestBody @Valid Client client) {
        return ResponseEntity.ok().body(this.clientService.createClient(client));
    }

    /**
     * Update client by id.
     *
     * @param id
     * @param client
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<Client> updateClient(@PathVariable long id, @RequestBody @Valid Client client) {
        client.setId(id);
        return ResponseEntity.ok().body(this.clientService.updateClient(client));
    }

    /**
     * Delete client by id.
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public HttpStatus deleteClient(@PathVariable long id) {
        this.clientService.deleteClient(id);
        return HttpStatus.OK;
    }

}
