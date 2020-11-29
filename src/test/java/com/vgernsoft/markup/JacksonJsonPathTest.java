package com.vgernsoft.markup;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class JacksonJsonPathTest {

    final Configuration JACKSON_CONFIG = Configuration.builder().jsonProvider(new JacksonJsonNodeJsonProvider())
            .mappingProvider(new JacksonMappingProvider()).build();

    @Test
    public void testFind() throws JsonMappingException, JsonProcessingException {

        // given
        final String path = "$..data";
        final JsonNode json = new ObjectMapper().readTree("{\"other\": 21, \"data\": { \"value\": \"10\"} }");

        // when
        ArrayNode stuff = JsonPath.using(JACKSON_CONFIG).parse(json).read(path);
        log.debug("stuff {}", stuff);

        // then
        assertNotNull(stuff);
    }
}
