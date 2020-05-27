package io.maverick.database.breeze;

import io.maverick.database.breeze.domain.TransactionalValueDTO;
import io.maverick.database.breeze.domain.ValueDTO;
import io.maverick.database.breeze.exception.BreezeActionException;
import io.maverick.database.breeze.service.BreezeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * Created by istvanvajnorak on 2020. 05. 26..
 *
 * A simple rest service endpoint to expose the key value service for remote consumption over HTTP/HTTPS.
 *
 */
@RestController
public class BreezeController {

    // The actual implementation of our key value service
    BreezeService<String,String> service;

    BreezeController(@Autowired BreezeService<String,String> service){
     this.service = service;
    }

    @GetMapping("/entry/{key}")
    public ResponseEntity<ValueDTO<String,String>> get(@PathVariable("key") String key){
        return createResponse(new ValueDTO<>(key, service.get(key)));
    }

    @GetMapping("/entry/{key}/transaction/{transactionId}")
    public ResponseEntity<TransactionalValueDTO> get(@PathVariable("key") String key,
                                                     @PathVariable("transactionId") String transactionId){
        return createResponse(new TransactionalValueDTO(transactionId,key,service.get(key,transactionId)));
    }

    @PostMapping("/entry/{key}")
    public ResponseEntity<String> put(@RequestBody ValueDTO<String,String> request,
                    @PathVariable("key") String key){
        service.put(key, request.getValue());
        return createResponse("UPSERTED");
    }

    @PostMapping("/entry/{key}/transaction/{transactionId}")
    public ResponseEntity<String> put(@RequestBody ValueDTO<String,String> request,
                    @PathVariable("key") String key,
                    @PathVariable("transactionId") String transactionId){
            service.put(key, request.getValue(),transactionId);
        return createResponse("DELETED");
    }

    @DeleteMapping("/entry/{key}")
    public ResponseEntity<String> delete(@PathVariable("key") String key) {
        service.delete(key);
        return createResponse("DELETED");
    }

    @DeleteMapping("/entry/{key}/transaction/{transactionId}")
    public ResponseEntity<String> delete(@PathVariable("key") String key,
                       @PathVariable("transactionId") String transactionId) {
            service.delete(key,transactionId);
        return createResponse("DELETED");
    }

    @PostMapping("/transaction/{transactionId}")
    public ResponseEntity<String> createTransaction(@PathVariable("transactionId") String transactionId){
            service.createTransaction(transactionId);
            return createResponse("CREATED");
    }

    @PostMapping("/transaction/{transactionId}/rollback")
    public ResponseEntity<String> rollbackTransaction(@PathVariable("transactionId") String transactionId){
            service.rollbackTransaction(transactionId);
            return createResponse("ROLLED BACK");
    }

    @PostMapping("/transaction/{transactionId}/commit")
    public ResponseEntity<String> commit(@PathVariable("transactionId") String transactionId){
        service.commitTransaction(transactionId);
        return createResponse("COMMITTED");
    }

    @ExceptionHandler({ BreezeActionException.class })
    public ResponseEntity<Object> handleAll(BreezeActionException ex) {
        return new ResponseEntity<>(
                ex.getContext(), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    private <T> ResponseEntity<T> createResponse(T response){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Content-Type", "application/json");
        return new ResponseEntity<T>(response, headers, HttpStatus.OK);
    }
}
