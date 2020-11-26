package com.vgernsoft.markup;

import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.Optional;
import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

class JsonNodeConverter implements AttributeConverter<JsonNode, String> {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(JsonNode jsonNode) {
        return jsonNode.toString();
    }

    @Override
    public JsonNode convertToEntityAttribute(String string) {
        try {
            return objectMapper.readTree(string);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

@Data
@NoArgsConstructor
class Markup {
    private JsonNode data;

    private JsonNode fired;

    private JsonNode result;
}

@Data
@Entity
@NoArgsConstructor
class MarkupRule {

    @Id
    private UUID id;

    private String rule;

    @Convert(converter = JsonNodeConverter.class)
    private JsonNode markup;
}

interface MarkupRuleRepository extends JpaRepository<MarkupRule, UUID> {
}

@Service
@RequiredArgsConstructor
class MarkupEngine {

    private final MarkupRuleRepository markupRuleRepository;

    public Markup markup(final JsonNode data) {
        Markup markup = new Markup();
        markupRuleRepository.findAll().forEach(mr -> { 
            JsonPath.parse(data).read(mr.getRule(), JsonNode.class);
        });
        return markup;
    }
} 

@RestController
@RequiredArgsConstructor
@RequestMapping("/markup")
class MarkupController {

    private final MarkupEngine markupEngine;

    @PostMapping()
    public ResponseEntity<Markup> markup(@RequestBody JsonNode data) {
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