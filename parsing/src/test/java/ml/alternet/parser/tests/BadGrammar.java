package ml.alternet.parser.tests;

import ml.alternet.parser.Grammar;
import static ml.alternet.parser.Grammar.*;

public interface BadGrammar extends Grammar {

    BadGrammar $ = $(); // should be the last field

    Token T = is('t');

}
