package com.oneff.customer.core.model;

import com.oneff.customer.core.exception.DocumentValidationException;

import java.util.Objects;

/**
 * Value object representing a Brazilian identification document (CPF or CNPJ).
 *
 * <p>The canonical constructor strips formatting (dots, dashes, slashes) and validates
 * that the resulting number has the correct digit count and valid check digits.
 * Invalid documents cannot be instantiated — validation happens at construction time.</p>
 *
 * @param type   whether this is a PF (CPF) or PJ (CNPJ) document
 * @param number the raw numeric string (11 digits for CPF, 14 for CNPJ)
 */
public record Document(CustomerType type, String number) {

    private static final int CPF_LENGTH = 11;
    private static final int CNPJ_LENGTH = 14;

    public Document {
        Objects.requireNonNull(type, "Document type must not be null");
        Objects.requireNonNull(number, "Document number must not be null");

        number = stripFormatting(number);

        if (type == CustomerType.PF) {
            validateCpf(number);
        } else {
            validateCnpj(number);
        }
    }

    /**
     * Returns the document formatted with standard Brazilian punctuation.
     * CPF: {@code 123.456.789-09}, CNPJ: {@code 12.345.678/0001-95}
     */
    public String formatted() {
        if (type == CustomerType.PF) {
            return number.substring(0, 3) + "." +
                   number.substring(3, 6) + "." +
                   number.substring(6, 9) + "-" +
                   number.substring(9);
        }
        return number.substring(0, 2) + "." +
               number.substring(2, 5) + "." +
               number.substring(5, 8) + "/" +
               number.substring(8, 12) + "-" +
               number.substring(12);
    }

    /**
     * Returns a masked version of the document for safe logging.
     * CPF example: ***.***.X47-25 — CNPJ example: **.***.**&#47;*0001-81
     */
    public String masked() {
        if (type == CustomerType.PF) {
            // Show last 4 digits: XXX.XXX.X##-##
            return "***.***.*" + number.substring(7, 9) + "-" + number.substring(9);
        }
        // Show last 6 digits: XX.XXX.XXX/####-##
        return "**.***.***/" + number.substring(8, 12) + "-" + number.substring(12);
    }

    private static String stripFormatting(String raw) {
        return raw.replaceAll("[.\\-/]", "");
    }

    private static void validateCpf(String number) {
        if (number.length() != CPF_LENGTH) {
            throw new DocumentValidationException(
                "CPF must have exactly %d digits, got %d".formatted(CPF_LENGTH, number.length()));
        }
        if (!number.matches("\\d+")) {
            throw new DocumentValidationException("CPF must contain only digits");
        }
        if (isAllSameDigit(number)) {
            throw new DocumentValidationException("CPF with all identical digits is invalid");
        }
        if (!isValidCpfChecksum(number)) {
            throw new DocumentValidationException("CPF checksum validation failed");
        }
    }

    private static void validateCnpj(String number) {
        if (number.length() != CNPJ_LENGTH) {
            throw new DocumentValidationException(
                "CNPJ must have exactly %d digits, got %d".formatted(CNPJ_LENGTH, number.length()));
        }
        if (!number.matches("\\d+")) {
            throw new DocumentValidationException("CNPJ must contain only digits");
        }
        if (isAllSameDigit(number)) {
            throw new DocumentValidationException("CNPJ with all identical digits is invalid");
        }
        if (!isValidCnpjChecksum(number)) {
            throw new DocumentValidationException("CNPJ checksum validation failed");
        }
    }

    private static boolean isAllSameDigit(String number) {
        return number.chars().distinct().count() == 1;
    }

    static boolean isValidCpfChecksum(String cpf) {
        int firstCheck = computeCpfDigit(cpf, 9);
        int secondCheck = computeCpfDigit(cpf, 10);
        return firstCheck == Character.getNumericValue(cpf.charAt(9))
            && secondCheck == Character.getNumericValue(cpf.charAt(10));
    }

    private static int computeCpfDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (length + 1 - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    static boolean isValidCnpjChecksum(String cnpj) {
        int firstCheck = computeCnpjDigit(cnpj, 12);
        int secondCheck = computeCnpjDigit(cnpj, 13);
        return firstCheck == Character.getNumericValue(cnpj.charAt(12))
            && secondCheck == Character.getNumericValue(cnpj.charAt(13));
    }

    private static int computeCnpjDigit(String cnpj, int length) {
        int[] weights = length == 12
            ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
            : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += Character.getNumericValue(cnpj.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
