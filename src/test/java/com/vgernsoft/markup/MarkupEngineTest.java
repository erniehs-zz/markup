package com.vgernsoft.markup;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class MarkupEngineTest {

    private final MarkupRule markupRule1 = MarkupRule.builder().id(UUID.randomUUID()).name("rule 1").trigger("$.value")
            .pattern("$.value").operation(OpCode.SUB).value("1").triggerOrder(1).build();
    private final MarkupRule markupRule2 = MarkupRule.builder().id(UUID.randomUUID()).name("rule 1")
            .trigger("$.data.value").pattern("$.data.value").operation(OpCode.MUL).value("10").triggerOrder(2).build();

    @Mock
    private MarkupRuleRepository markupRuleRepository;

    @InjectMocks
    private MarkupEngine markupEngine;

    @BeforeAll
    public void setUp() {
        when(markupRuleRepository.findAllByOrderByTriggerOrderAsc()).thenReturn(Arrays.asList(markupRule1, markupRule2));
    }

    @Test
    public void testMarkup() throws JsonMappingException, JsonProcessingException {

        // given
        JsonNode data = new ObjectMapper().readTree("{\"value\": 21, \"data\": {\"value\": 5, \"name\": \"ernie\"}}");

        // when
        MarkupResult markupResult = markupEngine.markup(data);
        log.debug("markupResult {}", markupResult);

        // then
        assertAll("markupResult", () -> assertNotNull(markupResult), () -> {
            assertNotNull(markupResult.getData());
            assertEquals(data, markupResult.getData());
        }, () -> {
            assertNotNull(markupResult.getFired());
            assertArrayEquals(markupResult.getFired().toArray(),
                    new UUID[] { markupRule1.getId(), markupRule2.getId() });
        }, () -> {
            assertNotNull(markupResult.getResult());
            assertEquals(new ObjectMapper().readTree("{\"value\": 20, \"data\": {\"value\": 50, \"name\": \"ernie\"}}"),
                    markupResult.getResult());
        });
    }
}
