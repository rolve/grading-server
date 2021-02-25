package ch.trick17.gradingserver.gradingservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1/status")
public class StatusController {

    @GetMapping
    public ResponseEntity<?> status() {
        return ResponseEntity.noContent().build();
    }
}
