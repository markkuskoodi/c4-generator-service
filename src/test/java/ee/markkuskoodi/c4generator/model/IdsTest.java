package ee.markkuskoodi.c4generator.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdsTest {

    @Test
    void identifiersAreDerivedFromCoordinatesOnly() {
        assertThat(Ids.system("Pet Clinic!")).isEqualTo("system:pet-clinic");
        assertThat(Ids.container("org.example", "billing-service"))
                .isEqualTo("container:org.example:billing-service");
        assertThat(Ids.dataStore("PostgreSQL", "petclinic"))
                .isEqualTo("datastore:postgresql:petclinic");
        assertThat(Ids.uses("container:a:b", "datastore:x:y"))
                .isEqualTo("container:a:b--uses--datastore:x:y");
    }

    @Test
    void identifiersAreStableAcrossInvocations() {
        assertThat(Ids.container("g", "a")).isEqualTo(Ids.container("g", "a"));
        assertThat(Ids.system("My System")).isEqualTo(Ids.system("My System"));
    }

    @Test
    void distinctCoordinatesYieldDistinctIdentifiers() {
        assertThat(Ids.container("g", "a")).isNotEqualTo(Ids.container("g", "b"));
        assertThat(Ids.dataStore("MySQL", "a")).isNotEqualTo(Ids.dataStore("MySQL", "b"));
    }

    @Test
    void emptySlugIsRejected() {
        assertThatThrownBy(() -> Ids.system("!!!")).isInstanceOf(IllegalArgumentException.class);
    }
}