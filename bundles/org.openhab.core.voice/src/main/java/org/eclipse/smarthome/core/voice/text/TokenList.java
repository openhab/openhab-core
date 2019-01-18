/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.voice.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A helper to parse a sequence of tokens. This class is immutable.
 *
 * @author Tilman Kamp - Initial contribution and API
 *
 */
public class TokenList {

    private List<String> list = null;

    private int head = 0;
    private int tail = 0;

    /**
     * Constructs a new instance.
     *
     * @param list of the initial tokens
     */
    public TokenList(List<String> list) {
        this.list = Collections.unmodifiableList(new ArrayList<String>(list));
        this.head = 0;
        this.tail = list.size() - 1;
    }

    private TokenList(List<String> list, int head, int tail) {
        this.list = list;
        this.head = head;
        this.tail = tail;
    }

    /**
     * Gets the first token of the list.
     *
     * @return the first token of the list
     */
    public String head() {
        return (list.size() < 1 || head < 0 || head >= list.size()) ? null : list.get(head);
    }

    /**
     * Gets the last token of the list.
     *
     * @return the last token of the list
     */
    public String tail() {
        return (list.size() < 1 || tail < 0 || tail >= list.size()) ? null : list.get(tail);
    }

    /**
     * Checks, if the list is empty.
     *
     * @return if the list is empty
     */
    public boolean eof() {
        return head > tail;
    }

    /**
     * Retrieves the token count within the list.
     *
     * @return token count
     */
    public int size() {
        return tail - head + 1;
    }

    /**
     * Checks for the first token of the list.
     * If it is equal to one of the provided alternatives, it will succeed.
     *
     * @param alternatives Allowed token values for the list's first token.
     *            If empty, all token values are allowed.
     * @return True, if first token is equal to one of the alternatives or if no alternatives were provided.
     *         False otherwise. Always false, if there is no first token (if the list is empty).
     */
    public boolean checkHead(String... alternatives) {
        return check(head, alternatives);
    }

    /**
     * Checks for the last token of the list.
     * If it is equal to one of the provided alternatives, it will succeed.
     *
     * @param alternatives Allowed token values for the list's last token.
     *            If empty, all token values are allowed.
     * @return True, if last token is equal to one of the alternatives or if no alternatives were provided.
     *         False otherwise. Always false, if there is no last token (if the list is empty).
     */
    public boolean checkTail(String... alternatives) {
        return check(tail, alternatives);
    }

    /**
     * Retrieves the first token of the list, in case it is equal to one of the provided alternatives.
     *
     * @param alternatives Allowed token values for the list's first token.
     *            If empty, all token values are allowed.
     * @return First token, if it is equal to one of the alternatives or if no alternatives were provided.
     *         Null otherwise. Always null, if there is no first token (if the list is empty).
     */
    public String peekHead(String... alternatives) {
        return peek(head, alternatives);
    }

    /**
     * Retrieves the last token of the list, in case it is equal to one of the provided alternatives.
     *
     * @param alternatives Allowed token values for the list's last token.
     *            If empty, all token values are allowed.
     * @return Last token, if it is equal to one of the alternatives or if no alternatives were provided.
     *         Null otherwise. Always null, if there is no last token (if the list is empty).
     */
    public String peekTail(String... alternatives) {
        return peek(tail, alternatives);
    }

    /**
     * Creates a new list without the first token.
     *
     * @return a new list without the first token
     */
    public TokenList skipHead() {
        return new TokenList(list, head + 1, tail);
    }

    /**
     * Creates a new list without the last token.
     *
     * @return a new list without the last token
     */
    public TokenList skipTail() {
        return new TokenList(list, head, tail - 1);
    }

    private String peek(int index, String... alternatives) {
        return splice(index, alternatives);
    }

    private boolean check(int index, String... alternatives) {
        return splice(index, alternatives) != null;
    }

    private String splice(int index, String... alternatives) {
        if (index < head || index > tail || head > tail) {
            return null;
        }
        String token = list.get(index);
        if (alternatives.length == 0) {
            return token;
        } else {
            for (String alt : alternatives) {
                if (alt.equals(token)) {
                    return token;
                }
            }
            return null;
        }
    }

}
