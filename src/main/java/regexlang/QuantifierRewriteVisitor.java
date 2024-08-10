package regexlang;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

import regexlang.SimpleRegexpParser.*;
import org.antlr.v4.runtime.*;

public class QuantifierRewriteVisitor extends SimpleRegexpBaseVisitor<ParserRuleContext> {

    /**
     * Rewrite unbounded counter quantifiers using the following rules:
     * 1. Map x{n,}? to x{n}x*?
     * 2. Map x{n,} to x{n}x*
     * 
     * Other quantifiers are left unchanged.
     * 
     * @param ctx The quantifier node to rewrite.
     * @return The concat node for the rewritten quantifier.
     */
    @Override
    public ParserRuleContext visitQuantifier(SimpleRegexpParser.QuantifierContext ctx) {
        if (ctx.unboundedCounter() == null) {
            // We only rewrite unbounded quantifiers.
            return visitChildren(ctx);
        } else {
            // Map x{n,}? to x{n}x*? or map x{n,} to x{n}x*, depending on the eagerness of
            // the counter

            QuantifiableContext x = ctx.quantifiable();
            ConcatContext concatCtx = new ConcatContext((ParserRuleContext) ctx.parent, ctx.parent.invokingState);
            // The concat node replaces the quantifier node being visited
            assert concatCtx.parent == ctx.parent;

            // Left child of concat will be x{n}
            QuantifierContext leftChild = new QuantifierContext(concatCtx, concatCtx.invokingState);
            concatCtx.addChild(leftChild);
            // x{n} will have two children: x and the exactCounter node
            QuantifierContext parent = leftChild;
            QuantifiableContext x1 = new QuantifiableContext(parent, parent.invokingState);
            parent.addChild(x1);
            x1.addChild(x.getPayload());
            ExactCounterContext exactCounter = new ExactCounterContext(parent, parent.invokingState);
            parent.addChild(exactCounter);
            // The instance variables for the bounds are defined in SimpleRegexp.g4
            exactCounter.exactBound = ctx.exactBound;

            // ----------------------------------------
            // Right child of concat will be x* or x*?
            QuantifierContext rightChild = new QuantifierContext(concatCtx, concatCtx.invokingState);
            concatCtx.addChild(rightChild);
            // x*/x*? will have two children: x and star (either greedy or lazy)
            parent = rightChild;
            QuantifiableContext x2 = new QuantifiableContext(parent, parent.invokingState);
            parent.addChild(x2);
            x2.addChild(x.getPayload());
            StarContext star = new StarContext(parent, parent.invokingState);
            // The Eagerness enum is defined in SimpleRegexp.g4
            star.egrns = ctx.egrns;
            TerminalNode starTerminal = star.egrns == Eagerness.GREEDY
                    ? new TerminalNodeImpl(new CommonToken(SimpleRegexpLexer.GREEDY_STAR, "*"))
                    : new TerminalNodeImpl(new CommonToken(SimpleRegexpLexer.LAZY_STAR, "*?"));
            star.addChild(starTerminal);
            parent.addChild(star);
            // ----------------------------------------

            // -------------Postconditions---------------
            // Concat should have two children: x{n} and either x* or x*?
            assert concatCtx.children.size() == 2;
            assert concatCtx.getChild(0) == leftChild;
            assert concatCtx.getChild(1) == rightChild;
            // x{n} should have two children: x and the exactCounter node
            assert leftChild.children.size() == 2;
            assert leftChild.quantifiable().getText().equals(x.getText());
            assert leftChild.exactCounter().exactBound == ctx.lowerBound;
            assert leftChild.exactBound == ctx.lowerBound;
            assert leftChild.parent == concatCtx;
            // x*? should have two children: x and star (either greedy or lazy)
            assert rightChild.children.size() == 2;
            assert rightChild.quantifiable().getText().equals(x.getText());
            assert rightChild.star().egrns == ctx.egrns;
            assert rightChild.parent == concatCtx;
            assert ctx.egrns == Eagerness.LAZY || ctx.egrns == Eagerness.GREEDY;
            // ----------------------------------------

            return concatCtx;
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
