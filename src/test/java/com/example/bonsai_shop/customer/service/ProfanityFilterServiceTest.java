package com.example.bonsai_shop.customer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ProfanityFilterServiceTest {

    private ProfanityFilterService profanityFilterService;

    @BeforeEach
    public void setUp() {
        profanityFilterService = new ProfanityFilterService();
    }

    @Test
    public void testRemoveAccents_NullInput() {
        // TC-UNIT-ProfanityFilter-001
        String result = profanityFilterService.removeAccents(null);
        assertEquals("", result);
    }

    @Test
    public void testRemoveAccents_EmptyInput() {
        // TC-UNIT-ProfanityFilter-002
        String result = profanityFilterService.removeAccents("");
        assertEquals("", result);
    }

    @Test
    public void testRemoveAccents_ValidAccentsAndD() {
        // TC-UNIT-ProfanityFilter-003
        String result = profanityFilterService.removeAccents("Trần Văn Điệp");
        assertEquals("Tran Van Diep", result);
    }

    @Test
    public void testContainsProfanity_CleanText() {
        // TC-UNIT-ProfanityFilter-004
        boolean result = profanityFilterService.containsProfanity("Cây bonsai này uốn rất nghệ thuật");
        assertFalse(result);
    }

    @Test
    public void testContainsProfanity_RawLowercase() {
        // TC-UNIT-ProfanityFilter-005
        boolean result = profanityFilterService.containsProfanity("đồ khốn kiếp dit cụ");
        assertTrue(result);
    }

    @Test
    public void testContainsProfanity_RawUppercaseAndMixed() {
        // TC-UNIT-ProfanityFilter-006
        boolean result = profanityFilterService.containsProfanity("Cây cảnh ĐM này");
        assertTrue(result);
    }

    @Test
    public void testContainsProfanity_Obfuscated() {
        // TC-UNIT-ProfanityFilter-007
        boolean result = profanityFilterService.containsProfanity("thằng b_u_o_i kia");
        assertTrue(result);
    }

    @Test
    public void testContainsProfanity_FalsePositives() {
        // TC-UNIT-ProfanityFilter-008
        boolean result = profanityFilterService.containsProfanity("buổi sáng ăn cơm ở Hải Hậu");
        assertFalse(result);
        
        boolean result2 = profanityFilterService.containsProfanity("lòng vòng quanh sân");
        assertFalse(result2);
    }

    @Test
    public void testContainsProfanity_HtmlTags() {
        // TC-UNIT-ProfanityFilter-009
        boolean result = profanityFilterService.containsProfanity("<div>đm</div>");
        assertTrue(result);
    }

    @Test
    public void testContainsProfanity_Urls() {
        // TC-UNIT-ProfanityFilter-010
        boolean result = profanityFilterService.containsProfanity("Xem link: https://google.com/dm");
        assertFalse(result);
    }

    @Test
    public void testContainsProfanity_ExtremelyLongText() {
        // TC-UNIT-ProfanityFilter-011
        String longText = "a".repeat(3000) + " đm";
        boolean result = profanityFilterService.containsProfanity(longText);
        assertTrue(result);
    }

    @Test
    public void testMaskProfanity_MultipleProfanities() {
        // TC-UNIT-ProfanityFilter-012
        String result = profanityFilterService.maskProfanity("đm cái con chó đẻ này, cút");
        assertEquals("*** cái con *** này, cút", result);
    }
}
