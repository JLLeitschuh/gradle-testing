package example;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class JavaSomething {

    private final String something;

    JavaSomething(String something) {
        this.something = something;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (this.getClass() != other.getClass()) return false;
        final JavaSomething javaOther = (JavaSomething) other;
        return something.equals(javaOther.something);
    }
}
