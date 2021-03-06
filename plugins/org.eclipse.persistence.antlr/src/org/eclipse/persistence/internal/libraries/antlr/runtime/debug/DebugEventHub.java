/*
 [The "BSD license"]
 Copyright (c) 2005-2009 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
     derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.eclipse.persistence.internal.libraries.antlr.runtime.debug;

import org.eclipse.persistence.internal.libraries.antlr.runtime.Token;
import org.eclipse.persistence.internal.libraries.antlr.runtime.RecognitionException;

import java.util.List;
import java.util.ArrayList;

/** Broadcast debug events to multiple listeners.  Lets you debug and still
 *  use the event mechanism to build parse trees etc...  Not thread-safe.
 *  Don't add events in one thread while parser fires events in another.
 *
 *  @see DebugEventRepeater
 */
public class DebugEventHub implements DebugEventListener {
    protected List<DebugEventListener> listeners = new ArrayList<DebugEventListener>();

    public DebugEventHub(DebugEventListener listener) {
        listeners.add(listener);
    }

    public DebugEventHub(DebugEventListener a, DebugEventListener b) {
        listeners.add(a);
        listeners.add(b);
    }

    /** Add another listener to broadcast events too.  Not thread-safe.
     *  Don't add events in one thread while parser fires events in another.
     */
    public void addListener(DebugEventListener listener) {
        listeners.add(listener);
    }

    /* To avoid a mess like this:
        public void enterRule(final String ruleName) {
            broadcast(new Code(){
                public void exec(DebugEventListener listener) {listener.enterRule(ruleName);}}
                );
        }
        I am dup'ing the for-loop in each.  Where are Java closures!? blech!
     */

    @Override
    public void enterRule(String grammarFileName, String ruleName) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.enterRule(grammarFileName,ruleName);
        }
    }

    @Override
    public void exitRule(String grammarFileName, String ruleName) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.exitRule(grammarFileName, ruleName);
        }
    }

    @Override
    public void enterAlt(int alt) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.enterAlt(alt);
        }
    }

    @Override
    public void enterSubRule(int decisionNumber) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.enterSubRule(decisionNumber);
        }
    }

    @Override
    public void exitSubRule(int decisionNumber) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.exitSubRule(decisionNumber);
        }
    }

    @Override
    public void enterDecision(int decisionNumber, boolean couldBacktrack) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.enterDecision(decisionNumber, couldBacktrack);
        }
    }

    @Override
    public void exitDecision(int decisionNumber) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.exitDecision(decisionNumber);
        }
    }

    @Override
    public void location(int line, int pos) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.location(line, pos);
        }
    }

    @Override
    public void consumeToken(Token token) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.consumeToken(token);
        }
    }

    @Override
    public void consumeHiddenToken(Token token) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.consumeHiddenToken(token);
        }
    }

    @Override
    public void LT(int index, Token t) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.LT(index, t);
        }
    }

    @Override
    public void mark(int index) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.mark(index);
        }
    }

    @Override
    public void rewind(int index) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.rewind(index);
        }
    }

    @Override
    public void rewind() {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.rewind();
        }
    }

    @Override
    public void beginBacktrack(int level) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.beginBacktrack(level);
        }
    }

    @Override
    public void endBacktrack(int level, boolean successful) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.endBacktrack(level, successful);
        }
    }

    @Override
    public void recognitionException(RecognitionException e) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.recognitionException(e);
        }
    }

    @Override
    public void beginResync() {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.beginResync();
        }
    }

    @Override
    public void endResync() {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.endResync();
        }
    }

    @Override
    public void semanticPredicate(boolean result, String predicate) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.semanticPredicate(result, predicate);
        }
    }

    @Override
    public void commence() {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.commence();
        }
    }

    @Override
    public void terminate() {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.terminate();
        }
    }


    // Tree parsing stuff

    @Override
    public void consumeNode(Object t) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.consumeNode(t);
        }
    }

    @Override
    public void LT(int index, Object t) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.LT(index, t);
        }
    }


    // AST Stuff

    @Override
    public void nilNode(Object t) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.nilNode(t);
        }
    }

    @Override
    public void errorNode(Object t) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.errorNode(t);
        }
    }

    @Override
    public void createNode(Object t) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.createNode(t);
        }
    }

    @Override
    public void createNode(Object node, Token token) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.createNode(node, token);
        }
    }

    @Override
    public void becomeRoot(Object newRoot, Object oldRoot) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.becomeRoot(newRoot, oldRoot);
        }
    }

    @Override
    public void addChild(Object root, Object child) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.addChild(root, child);
        }
    }

    @Override
    public void setTokenBoundaries(Object t, int tokenStartIndex, int tokenStopIndex) {
        for (int i = 0; i < listeners.size(); i++) {
            DebugEventListener listener = listeners.get(i);
            listener.setTokenBoundaries(t, tokenStartIndex, tokenStopIndex);
        }
    }
}
