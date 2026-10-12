package com.example.sample.messaging;

import com.example.sample.service.EmployeeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EmployeeTerminationListener {

    private static final Logger log = LoggerFactory.getLogger(EmployeeTerminationListener.class);

    private final EmployeeService employeeService;

    public EmployeeTerminationListener(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @KafkaListener(topics = "${app.kafka.topics.employee-terminations}", groupId = "sample-terminations")
    public void onTermination(EmployeeTerminationEvent event) {
        log.info("Processing termination for employee {}", event.employeeId());
        employeeService.terminate(event);
    }
}
