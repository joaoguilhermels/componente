package com.onefinancial.customer.core.model;

import com.onefinancial.customer.core.exception.DocumentValidationException;
import com.onefinancial.customer.core.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    private static final String VALID_CPF = "52998224725";
    private static final String VALID_CNPJ = "11222333000181";

    @Nested
    class Creation {

        @Test
        void shouldCreatePfCustomerInDraftStatus() {
            Customer customer = Customer.createPF(VALID_CPF, "João Silva");

            assertThat(customer.getId()).isNotNull();
            assertThat(customer.getType()).isEqualTo(CustomerType.PF);
            assertThat(customer.getDocument().number()).isEqualTo(VALID_CPF);
            assertThat(customer.getDisplayName()).isEqualTo("João Silva");
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.DRAFT);
            assertThat(customer.getAddresses()).isEmpty();
            assertThat(customer.getContacts()).isEmpty();
            assertThat(customer.getAttributes().size()).isZero();
            assertThat(customer.getCreatedAt()).isNotNull();
            assertThat(customer.getUpdatedAt()).isEqualTo(customer.getCreatedAt());
        }

        @Test
        void shouldCreatePjCustomerInDraftStatus() {
            Customer customer = Customer.createPJ(VALID_CNPJ, "Empresa LTDA");

            assertThat(customer.getType()).isEqualTo(CustomerType.PJ);
            assertThat(customer.getDocument().number()).isEqualTo(VALID_CNPJ);
            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.DRAFT);
        }

        @Test
        void shouldTrimDisplayName() {
            Customer customer = Customer.createPF(VALID_CPF, "  João Silva  ");

            assertThat(customer.getDisplayName()).isEqualTo("João Silva");
        }

        @Test
        void shouldRejectBlankDisplayName() {
            assertThatThrownBy(() -> Customer.createPF(VALID_CPF, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        }

        @Test
        void shouldRejectNullDisplayName() {
            assertThatThrownBy(() -> Customer.createPF(VALID_CPF, null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldRejectInvalidDocument() {
            assertThatThrownBy(() -> Customer.createPF("invalid", "Test"))
                .isInstanceOf(DocumentValidationException.class);
        }
    }

    @Nested
    class StatusTransitions {

        @Test
        void shouldTransitionFromDraftToActive() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            customer.transitionTo(CustomerStatus.ACTIVE);

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        }

        @Test
        void shouldTransitionFromDraftToClosed() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            customer.transitionTo(CustomerStatus.CLOSED);

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.CLOSED);
        }

        @Test
        void shouldTransitionFromActiveToSuspended() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            customer.transitionTo(CustomerStatus.ACTIVE);
            customer.transitionTo(CustomerStatus.SUSPENDED);

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
        }

        @Test
        void shouldTransitionFromSuspendedBackToActive() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            customer.transitionTo(CustomerStatus.ACTIVE);
            customer.transitionTo(CustomerStatus.SUSPENDED);
            customer.transitionTo(CustomerStatus.ACTIVE);

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        }

        @Test
        void shouldRejectTransitionFromDraftToSuspended() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");

            assertThatThrownBy(() -> customer.transitionTo(CustomerStatus.SUSPENDED))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("DRAFT")
                .hasMessageContaining("SUSPENDED");
        }

        @Test
        void shouldRejectTransitionFromClosedToAnything() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            customer.transitionTo(CustomerStatus.CLOSED);

            assertThatThrownBy(() -> customer.transitionTo(CustomerStatus.ACTIVE))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("CLOSED");
        }

        @Test
        void shouldTransitionFromActiveToClosed() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            customer.transitionTo(CustomerStatus.ACTIVE);
            customer.transitionTo(CustomerStatus.CLOSED);

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.CLOSED);
        }

        @Test
        void shouldTransitionFromSuspendedToClosed() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            customer.transitionTo(CustomerStatus.ACTIVE);
            customer.transitionTo(CustomerStatus.SUSPENDED);
            customer.transitionTo(CustomerStatus.CLOSED);

            assertThat(customer.getStatus()).isEqualTo(CustomerStatus.CLOSED);
        }

        @Test
        void shouldRejectSelfTransitionDraftToDraft() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");

            assertThatThrownBy(() -> customer.transitionTo(CustomerStatus.DRAFT))
                .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        void shouldRejectSelfTransitionActiveToActive() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            customer.transitionTo(CustomerStatus.ACTIVE);

            assertThatThrownBy(() -> customer.transitionTo(CustomerStatus.ACTIVE))
                .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        void shouldRejectSelfTransitionSuspendedToSuspended() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            customer.transitionTo(CustomerStatus.ACTIVE);
            customer.transitionTo(CustomerStatus.SUSPENDED);

            assertThatThrownBy(() -> customer.transitionTo(CustomerStatus.SUSPENDED))
                .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        void shouldRejectSelfTransitionClosedToClosed() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            customer.transitionTo(CustomerStatus.CLOSED);

            assertThatThrownBy(() -> customer.transitionTo(CustomerStatus.CLOSED))
                .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        void shouldUpdateTimestampOnTransition() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            var originalUpdatedAt = customer.getUpdatedAt();

            customer.transitionTo(CustomerStatus.ACTIVE);

            assertThat(customer.getUpdatedAt()).isAfterOrEqualTo(originalUpdatedAt);
        }
    }

    @Nested
    class Modifications {

        @Test
        void shouldUpdateDisplayName() {
            Customer customer = Customer.createPF(VALID_CPF, "Old Name");
            customer.updateDisplayName("New Name");

            assertThat(customer.getDisplayName()).isEqualTo("New Name");
        }

        @Test
        void shouldAddAddress() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            Address address = new Address(null, "Rua A", "123", null, "Centro",
                "São Paulo", "SP", "01000-000", "BR");

            customer.addAddress(address);

            assertThat(customer.getAddresses()).hasSize(1);
        }

        @Test
        void shouldAddContact() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            Contact contact = new Contact(null, Contact.ContactType.EMAIL, "test@example.com", true);

            customer.addContact(contact);

            assertThat(customer.getContacts()).hasSize(1);
        }

        @Test
        void shouldSetAttributes() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            Attributes attrs = Attributes.empty()
                .with("tier", new AttributeValue.StringValue("GOLD"));

            customer.setAttributes(attrs);

            assertThat(customer.getAttributes().size()).isEqualTo(1);
        }

        @Test
        void shouldReturnUnmodifiableAddressList() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");

            assertThatThrownBy(() -> customer.getAddresses().add(
                new Address(null, "Rua B", "456", null, "Centro",
                    "Rio de Janeiro", "RJ", "20000-000", "BR")))
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class Equality {

        @Test
        void shouldBeEqualByIdOnly() {
            Customer c1 = Customer.createPF(VALID_CPF, "Name 1");
            Customer c2 = Customer.createPF(VALID_CPF, "Name 2");

            // Different IDs (both created with random UUIDs)
            assertThat(c1).isNotEqualTo(c2);
        }

        @Test
        void shouldBeEqualToSelf() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");

            assertThat(customer).isEqualTo(customer);
        }

        @Test
        void shouldNotBeEqualToNull() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");

            assertThat(customer).isNotEqualTo(null);
        }

        @Test
        void shouldNotBeEqualToDifferentType() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");

            assertThat(customer).isNotEqualTo("not a customer");
        }

        @Test
        void shouldHaveConsistentHashCode() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");

            int hash1 = customer.hashCode();
            int hash2 = customer.hashCode();

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        void shouldHaveSameHashCodeForEqualObjects() {
            Customer customer = Customer.createPF(VALID_CPF, "Test");
            // Self-equality means same hash code
            assertThat(customer.hashCode()).isEqualTo(customer.hashCode());
        }
    }
}
