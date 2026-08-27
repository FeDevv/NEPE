package org.nepe.settings.adapter.out;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * JPA Entity mapping for the key-value {@code settings} database table.
 * <p>
 * Resides exclusively within the outbound adapter layer of the settings feature.
 */
@Entity
@Table(name = "settings")
public class SettingsJpaEntity {

    @Id
    @Column(name = "setting_key", nullable = false, length = 50)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 255)
    private String value;

    /**
     * Default no-args constructor required by JPA / Hibernate.
     */
    protected SettingsJpaEntity() {
    }

    /**
     * Full constructor for creating or updating a setting key-value pair.
     */
    public SettingsJpaEntity(String key, String value) {
        this.key = key;
        this.value = value;
    }

    // --- Getters and Setters ---

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    // --- Identity, Equality and Diagnostics ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SettingsJpaEntity that = (SettingsJpaEntity) o;
        return Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return "SettingsJpaEntity{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
