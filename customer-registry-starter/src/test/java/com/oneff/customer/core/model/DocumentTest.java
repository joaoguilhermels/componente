package com.oneff.customer.core.model;

import com.oneff.customer.core.exception.DocumentValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTest {

    // Valid CPFs (real checksum-valid numbers for testing)
    private static final String VALID_CPF = "52998224725";
    private static final String VALID_CPF_FORMATTED = "529.982.247-25";

    // Valid CNPJs
    private static final String VALID_CNPJ = "11222333000181";
    private static final String VALID_CNPJ_FORMATTED = "11.222.333/0001-81";

    @Nested
    class CpfValidation {

        @Test
        void shouldAcceptValidCpfWithElevenDigits() {
            var doc = new Document(CustomerType.PF, VALID_CPF);

            assertThat(doc.number()).isEqualTo(VALID_CPF);
            assertThat(doc.type()).isEqualTo(CustomerType.PF);
        }

        @Test
        void shouldStripFormattingBeforeValidation() {
            var doc = new Document(CustomerType.PF, VALID_CPF_FORMATTED);

            assertThat(doc.number()).isEqualTo(VALID_CPF);
        }

        @Test
        void shouldRejectCpfWithWrongDigitCount() {
            assertThatThrownBy(() -> new Document(CustomerType.PF, "1234567890"))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("11 digits");
        }

        @Test
        void shouldRejectCpfWithTooManyDigits() {
            assertThatThrownBy(() -> new Document(CustomerType.PF, "123456789012"))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("11 digits");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "00000000000", "11111111111", "22222222222",
            "33333333333", "44444444444", "55555555555",
            "66666666666", "77777777777", "88888888888", "99999999999"
        })
        void shouldRejectCpfWithAllIdenticalDigits(String cpf) {
            assertThatThrownBy(() -> new Document(CustomerType.PF, cpf))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("identical digits");
        }

        @Test
        void shouldRejectCpfWithInvalidChecksum() {
            assertThatThrownBy(() -> new Document(CustomerType.PF, "52998224726"))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("checksum");
        }

        @Test
        void shouldFormatCpfCorrectly() {
            var doc = new Document(CustomerType.PF, VALID_CPF);

            assertThat(doc.formatted()).isEqualTo(VALID_CPF_FORMATTED);
        }

        @Test
        void shouldRejectNonNumericCpf() {
            assertThatThrownBy(() -> new Document(CustomerType.PF, "5299822472A"))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("only digits");
        }
    }

    @Nested
    class CnpjValidation {

        @Test
        void shouldAcceptValidCnpjWithFourteenDigits() {
            var doc = new Document(CustomerType.PJ, VALID_CNPJ);

            assertThat(doc.number()).isEqualTo(VALID_CNPJ);
            assertThat(doc.type()).isEqualTo(CustomerType.PJ);
        }

        @Test
        void shouldStripFormattingFromCnpj() {
            var doc = new Document(CustomerType.PJ, VALID_CNPJ_FORMATTED);

            assertThat(doc.number()).isEqualTo(VALID_CNPJ);
        }

        @Test
        void shouldRejectCnpjWithWrongDigitCount() {
            assertThatThrownBy(() -> new Document(CustomerType.PJ, "1122233300018"))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("14 digits");
        }

        @Test
        void shouldRejectCnpjWithInvalidChecksum() {
            assertThatThrownBy(() -> new Document(CustomerType.PJ, "11222333000182"))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("checksum");
        }

        @Test
        void shouldFormatCnpjCorrectly() {
            var doc = new Document(CustomerType.PJ, VALID_CNPJ);

            assertThat(doc.formatted()).isEqualTo(VALID_CNPJ_FORMATTED);
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "00000000000000", "11111111111111"
        })
        void shouldRejectCnpjWithAllIdenticalDigits(String cnpj) {
            assertThatThrownBy(() -> new Document(CustomerType.PJ, cnpj))
                .isInstanceOf(DocumentValidationException.class)
                .hasMessageContaining("identical digits");
        }
    }

    @Nested
    class NullSafety {

        @Test
        void shouldRejectNullType() {
            assertThatThrownBy(() -> new Document(null, VALID_CPF))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldRejectNullNumber() {
            assertThatThrownBy(() -> new Document(CustomerType.PF, null))
                .isInstanceOf(NullPointerException.class);
        }
    }
}
