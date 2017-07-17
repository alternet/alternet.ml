package ml.alternet.util;

import ml.alternet.util.StringBuilderUtil;

import java.util.Arrays;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class StringBuilderUtilTest {

    @Test
    public void stringBuilderCollector_Should_joinNames() {
        List<String> persons = Arrays.asList("Paul", "Brad", "Alice", "Jack");
        StringBuilder buffer = new StringBuilder("People : ");
        persons.stream().collect(StringBuilderUtil.collectorOf("[", "; ", "]", buffer));
        Assertions.assertThat(buffer.toString()).isEqualTo("People : [Paul; Brad; Alice; Jack]");
    }

    public static class Person {
        String firstName;
        String lastName;
        public Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
        public StringBuilder prettyPrint(StringBuilder buf) {
            return buf.append(firstName).append(' ').append(lastName);
        }
    }

    @Test
    public void stringBuilderCollector_Should_joinPersons() {
        List<Person> persons = Arrays.asList(new Person("Paul", "McCartney"), new Person("Brad", "Pitt"),
                new Person("Alice", "Cooper"), new Person("Jack", "Nicholson"));
        StringBuilder buffer = new StringBuilder("People : ");
        persons.stream().collect(StringBuilderUtil.collectorOf(
            "[", "; ", "]", buffer,
            person -> person.prettyPrint(buffer)
        ));
        Assertions.assertThat(buffer.toString()).isEqualTo("People : [Paul McCartney; Brad Pitt; Alice Cooper; Jack Nicholson]");
    }

}
