package com.vgernsoft.markup;

import java.util.*;
import java.util.stream.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.JsonPath;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import lombok.*;
import lombok.extern.slf4j.*;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

enum OpCode {
    ADD, SUB, MUL;

    public double exec(double a, double b) {
        switch (this) {
            case ADD:
                return a + b;
            case MUL:
                return a * b;
            case SUB:
                return a - b;
            default:
                return 0;
        }
    }
}

@Data
@Entity
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PACKAGE)
class MarkupRule {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @JsonProperty(access = Access.READ_ONLY)
    private UUID id;

    private String name;

    private String trigger;

    private String pattern;

    private OpCode operation;

    private String value;

    private Integer triggerOrder;
}

@Data
@Builder
class MarkupResult {
    private JsonNode data;

    private List<UUID> fired;

    private JsonNode result;
}

interface MarkupRuleRepository extends JpaRepository<MarkupRule, UUID> {

    List<MarkupRule> findAllByOrderByTriggerOrderAsc();
}

@Slf4j
@Service
@RequiredArgsConstructor
class MarkupEngine {

    private final com.jayway.jsonpath.Configuration jacksonJsonPathConfiguration;

    private final MarkupRuleRepository markupRuleRepository;

    public MarkupResult markup(final JsonNode data) {
        // TODO implement actual trigger to change data based upon opcode and pattern
        log.debug("requesting markup for {}", data);
        return MarkupResult.builder().data(data)
                .fired(markupRuleRepository.findAllByOrderByTriggerOrderAsc().stream()
                        .filter(mr -> !((ArrayNode) JsonPath.using(jacksonJsonPathConfiguration).parse(data)
                                .read(mr.getTrigger())).isEmpty())
                        .map(MarkupRule::getId).collect(Collectors.toList()))
                .result(data.deepCopy()).build();
    }
}

@RestController
@RequiredArgsConstructor
@RequestMapping("/markup")
class MarkupController {

    private final MarkupEngine markupEngine;

    @PostMapping()
    public ResponseEntity<MarkupResult> markup(@RequestBody JsonNode data) {
        return ResponseEntity.ok(markupEngine.markup(data));
    }
}

@RestController
@RequiredArgsConstructor
@RequestMapping("/markup-admin")
class MarkupAdminController {

    private final MarkupRuleRepository markupRuleRepository;

    @GetMapping(value = "/{id}")
    public ResponseEntity<MarkupRule> findById(@PathVariable UUID id) {
        return markupRuleRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping()
    public ResponseEntity<List<MarkupRule>> findAll() {
        return ResponseEntity.ok(markupRuleRepository.findAll());
    }

    @Operation(summary = "add markup", description = "add a new markup to the engine")
    @PostMapping()
    public ResponseEntity<MarkupRule> create(@RequestBody MarkupRule markupRule) {
        return ResponseEntity.ok(markupRuleRepository.save(markupRule));
    }

    @DeleteMapping()
    public ResponseEntity<MarkupRule> delete(@PathVariable UUID id) {
        Optional<MarkupRule> markupRule = markupRuleRepository.findById(id);
        markupRule.ifPresent(markupRuleRepository::delete);
        return markupRule.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }
}

@EnableSwagger2
@Configuration
class SpringFoxConfig {

    @Bean
    public com.jayway.jsonpath.Configuration jacksonJsonPathConfiguration() {
        return com.jayway.jsonpath.Configuration.builder().jsonProvider(new JacksonJsonNodeJsonProvider())
                .mappingProvider(new JacksonMappingProvider()).build();
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2).select()
                .apis(RequestHandlerSelectors.basePackage("com.vgernsoft.markup")).paths(PathSelectors.any()).build();
    }
}

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}