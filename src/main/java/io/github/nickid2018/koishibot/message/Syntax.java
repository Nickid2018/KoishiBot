package io.github.nickid2018.koishibot.message;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(SyntaxCollection.class)
public @interface Syntax {

    String syntax();
    String help();
    String rem() default "";


}
