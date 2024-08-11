package regexlang;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import regexlang.SimpleRegexpParser.RegexpContext;

import org.antlr.v4.runtime.ParserRuleContext;

public class Regexp extends ParserRuleContext {
    private ParseTree root;
    public Regexp(ParseTree tree) {
        super();
    }

    public Regexp(String regex) {
        super();
        SimpleRegexpParser parser = QuantExprRewriteVisitor.makeParser(regex);
        this.root = parser.regexp();
    }

    private static class CounterListener extends SimpleRegexpBaseListener {
        public boolean regexContainsCounter = false;
        @Override
        public void enterQuantExpr(SimpleRegexpParser.QuantExprContext ctx) {
            if (ctx.quantifier().unboundedCounter() != null || ctx.quantifier().boundedCounter() != null || ctx.quantifier().exactCounter() != null) {
              regexContainsCounter = true;
            }
        }
    } 

    public boolean containsCounter() {
        CounterListener listener = new CounterListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, this.root);
        assert this.root instanceof RegexpContext;
        listener.enterRegexp((RegexpContext) this.root);
        return listener.regexContainsCounter;
    }

}
