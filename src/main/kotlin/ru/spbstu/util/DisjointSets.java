package ru.spbstu.util;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DisjointSets<E> {
    protected Map<E, Node> map;

    public DisjointSets() {
        this.map = new HashMap();
    }

    public DisjointSets(int initialCapacity) {
        this.map = new HashMap(4 * initialCapacity / 3 + 1);
    }

    public boolean add(E e) {
        DisjointSets.Node x = (DisjointSets.Node)this.map.get(e);
        if (x != null) {
            return false;
        } else {
            this.map.put(e, new DisjointSets.Node());
            return true;
        }
    }

    public boolean inSameSet(Object e1, Object e2) {
        DisjointSets.Node x1 = (DisjointSets.Node)this.map.get(e1);
        if (x1 == null) {
            return false;
        } else {
            DisjointSets.Node x2 = (DisjointSets.Node)this.map.get(e2);
            if (x2 == null) {
                return false;
            } else {
                return x1.root() == x2.root();
            }
        }
    }

    public boolean union(Object e1, Object e2) {
        DisjointSets.Node x1 = (DisjointSets.Node)this.map.get(e1);
        if (x1 == null) {
            return false;
        } else {
            DisjointSets.Node x2 = (DisjointSets.Node)this.map.get(e2);
            return x2 == null ? false : x1.join(x2);
        }
    }

    public boolean contains(Object e) {
        return this.map.get(e) != null;
    }

    public void clear() {
        DisjointSets.Node node;
        for(Iterator i$ = this.map.values().iterator(); i$.hasNext(); node.parent = null) {
            node = (DisjointSets.Node)i$.next();
        }

        this.map.clear();
    }

    public Object root(Object e) { return this.map.get(e).root(); }

    protected static class Node {
        DisjointSets.Node parent = this;
        int rank = 0;

        protected Node() {
        }

        protected DisjointSets.Node root() {
            if (this != this.parent) {
                this.parent = this.parent.root();
            }

            return this.parent;
        }

        protected boolean join(DisjointSets.Node node) {
            DisjointSets.Node x = this.root();
            DisjointSets.Node y = node.root();
            if (x == y) {
                return false;
            } else {
                if (x.rank > y.rank) {
                    y.parent = x;
                } else {
                    x.parent = y;
                    if (x.rank == y.rank) {
                        ++y.rank;
                    }
                }

                return true;
            }
        }
    }
}
