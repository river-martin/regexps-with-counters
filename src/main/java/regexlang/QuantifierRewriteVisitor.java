package regexlang;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import regexlang.SimpleRegexpParser.UnboundedGreedyCounterContext;
import regexlang.SimpleRegexpParser.UnboundedLazyCounterContext;
import regexlang.SimpleRegexpParser.ConcatContext;
import regexlang.SimpleRegexpParser.QuantifiableContext;

public class QuantifierRewriteVisitor extends SimpleRegexpBaseVisitor<ParserRuleContext> {

    @Override
    public ParserRuleContext visitQuantifier(SimpleRegexpParser.QuantifierContext ctx) {
        QuantifiableContext quantifiableCtx = new QuantifiableContext(ctx, ctx.invokingState);
        visitQuantifiable(quantifiableCtx);
        if (ctx.unboundedLazyCounter() != null) {
            UnboundedLazyCounterContext unboundedLazyCounterCtx = ctx.unboundedLazyCounter();
            ConcatContext concatCtx = (ConcatContext) visitUnboundedLazyCounter(unboundedLazyCounterCtx);
            return concatCtx;
        } else if (ctx.unboundedGreedyCounter() != null) {
            UnboundedGreedyCounterContext unboundedGreedyCounterCtx = ctx.unboundedGreedyCounter();
            ConcatContext concatCtx = (ConcatContext) visitUnboundedGreedyCounter(unboundedGreedyCounterCtx);
            return concatCtx;
        } else {
            return visitChildren(ctx);
        }
    }

    public static ParserRuleContext rewrite(String input) {
        SimpleRegexpLexer lexer = new SimpleRegexpLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SimpleRegexpParser parser = new SimpleRegexpParser(tokens);
        SimpleRegexpParser.RegexpContext tree = parser.regexp();
        QuantifierRewriteVisitor visitor = new QuantifierRewriteVisitor();
        return visitor.visit(tree);
    }

}
