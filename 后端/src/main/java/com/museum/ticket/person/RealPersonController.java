package com.museum.ticket.person;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/real-persons")
public class RealPersonController {
    private final RealPersonService realPersonService;

    public RealPersonController(RealPersonService realPersonService) {
        this.realPersonService = realPersonService;
    }

    @GetMapping
    public List<RealPersonResponse> list() {
        return realPersonService.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RealPersonResponse create(@Valid @RequestBody RealPersonRequest request) {
        return realPersonService.create(request);
    }

    @PutMapping("/{personId}")
    public RealPersonResponse update(@PathVariable String personId,
                                     @Valid @RequestBody RealPersonRequest request) {
        return realPersonService.update(personId, request);
    }

    @DeleteMapping("/{personId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String personId) {
        realPersonService.delete(personId);
    }
}
