package regexlang;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.antlr.v4.runtime.tree.Trees;

import regexlang.SimpleRegexpParser.*;
import org.antlr.v4.runtime.*;

public class QuantifierRewriteVisitor extends SimpleRegexpBaseVisitor<ParseTree> {

    /**
     * Visit all children of a node and return the node itself.
     * 
     * @param node The node to visit.
     * @return The node itself.
     */
    @Override
    public ParseTree visitChildren(RuleNode node) {
        super.visitChildren(node);
        return node;
    }

    /**
     * Rewrite expressions with unbounded counters using the following rules:
     * 1. Map x{n,}? to x{n}x*?
     * 2. Map x{n,} to x{n}x*
     * 
     * Expressions with other quantifiers are left unchanged.
     * 
     * @param ctx The quantification expression node to rewrite.
     * @return The concat node for the rewritten quantification expression.
     */
    @Override
    public ParseTree visitQuantExpr(SimpleRegexpParser.QuantExprContext ctx) {
        super.visitChildren(ctx);
        if (ctx.quantifier().unboundedCounter() == null) {
            // We only rewrite expressions with unbounded quantifiers.
            return ctx;
        } else {
            // Map x{n,}? to x{n}x*? or map x{n,} to x{n}x*, depending on the eagerness of
            // the counter

            QuantifiableContext x = ctx.quantifiable();
            ConcatContext concatCtx = new ConcatContext((ParserRuleContext) ctx.parent, ctx.parent.invokingState);
            // The concat node replaces the quantifier node being visited
            assert concatCtx.parent == ctx.parent;

            // Left child of concat will be x{n}
            QuantExprContext leftChild = new QuantExprContext(concatCtx, concatCtx.invokingState);
            concatCtx.addChild(leftChild);
            // x{n} will have two children: x and the exactCounter node
            QuantExprContext parent = leftChild;
            QuantifiableContext x1 = new QuantifiableContext(parent, parent.invokingState);
            parent.addChild(x1);
            x1.addChild(x.getPayload());
            ExactCounterContext exactCounter = new ExactCounterContext(parent, parent.invokingState);
            parent.addChild(exactCounter);
            // The instance variables for the bounds are defined in SimpleRegexp.g4
            exactCounter.exactBound = ctx.quantifierCtx.exactBound;

            // ----------------------------------------
            // Right child of concat will be x* or x*?
            QuantExprContext rightChild = new QuantExprContext(concatCtx, concatCtx.invokingState);
            concatCtx.addChild(rightChild);
            // x*/x*? will have two children: x and star (either greedy or lazy)
            parent = rightChild;
            QuantifiableContext x2 = new QuantifiableContext(parent, parent.invokingState);
            parent.addChild(x2);
            x2.addChild(x.getPayload());
            StarContext star = new StarContext(parent, parent.invokingState);
            // The Eagerness enum is defined in SimpleRegexp.g4
            star.egrns = ctx.quantifierCtx.egrns;
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
            // x{n} should have two children: x and an exactCounter quantifier
            assert leftChild.children.size() == 2;
            assert leftChild.quantifiableCtx.getText().equals(x.getText());
            assert leftChild.quantifierCtx.exactBound == ctx.quantifierCtx.exactBound;
            assert leftChild.quantifierCtx.exactBound == leftChild.quantifierCtx.exactCounter().exactBound;
            assert leftChild.parent == concatCtx;
            // x*? should have two children: x and a star quantifier (either greedy or lazy)
            assert rightChild.children.size() == 2;
            assert rightChild.quantifiableCtx.getText().equals(x.getText());
            assert rightChild.quantifierCtx.egrns == ctx.quantifierCtx.egrns;
            assert rightChild.parent == concatCtx;
            assert ctx.quantifierCtx.egrns == Eagerness.LAZY || ctx.quantifierCtx.egrns == Eagerness.GREEDY;
            // ----------------------------------------

            return concatCtx;
        }
    }

    public static ParseTree rewrite(ParseTree tree) {
        QuantifierRewriteVisitor visitor = new QuantifierRewriteVisitor();
        return visitor.visit(tree);
    }

    public static ParseTree parse(String input) {
        SimpleRegexpLexer lexer = new SimpleRegexpLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SimpleRegexpParser parser = new SimpleRegexpParser(tokens);
        return parser.regexp();
    }

    public static void main(String[] args) {
        String input = "a{3,5}b{2,4}c{1,3} b*?";
        SimpleRegexpLexer lexer = new SimpleRegexpLexer(CharStreams.fromString(input));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SimpleRegexpParser parser = new SimpleRegexpParser(tokens);
        ParseTree tree = parser.regexp();
        System.out.println(tree.getText());
        ParseTree rewrittenTree = QuantifierRewriteVisitor.rewrite(tree);
        System.out.println(Trees.toStringTree(tree, parser));
        System.out.println(Trees.toStringTree(rewrittenTree, parser));
    }

}
