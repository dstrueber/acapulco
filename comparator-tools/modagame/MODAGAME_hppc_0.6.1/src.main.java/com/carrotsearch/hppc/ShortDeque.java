package com.carrotsearch.hppc;

import java.util.Iterator;
import java.util.List;

import com.carrotsearch.hppc.cursors.ShortCursor;
import com.carrotsearch.hppc.predicates.ShortPredicate;
import com.carrotsearch.hppc.procedures.ShortProcedure;


/**
 * A double-linked queue of <code>short</code>s.
 */
 @javax.annotation.Generated(date = "2014-09-08T10:42:29+0200", value = "HPPC generated from: ShortDeque.java") 
public interface ShortDeque extends ShortCollection
{
    /**
     * Removes the first element that equals <code>e1</code>, returning its 
     * deleted position or <code>-1</code> if the element was not found.   
     */
    public int removeFirstOccurrence(short e1);
    
    /**
     * Removes the last element that equals <code>e1</code>, returning its 
     * deleted position or <code>-1</code> if the element was not found.   
     */
    public int removeLastOccurrence(short e1);

    /**
     * Inserts the specified element at the front of this deque.
     *
     * @param e1 the element to add
     */
    public void addFirst(short e1);

    /**
     * Inserts the specified element at the end of this deque.
     *
     * @param e1 the element to add
     */
    public void addLast(short e1);
    
    /**
     * Retrieves and removes the first element of this deque.
     *
     * @return the head element of this deque.
     * @throws AssertionError if this deque is empty and assertions are enabled.
     */
    public short removeFirst();
    
    /**
     * Retrieves and removes the last element of this deque.
     *
     * @return the tail of this deque.
     * @throws AssertionError if this deque is empty and assertions are enabled.
     */
    public short removeLast();
    
    /**
     * Retrieves, but does not remove, the first element of this deque.
     *
     * @return the head of this deque.
     * @throws AssertionError if this deque is empty and assertions are enabled.
     */
    public short getFirst();

    /**
     * Retrieves, but does not remove, the last element of this deque.
     *
     * @return the tail of this deque.
     * @throws AssertionError if this deque is empty and assertions are enabled.
     */
    public short getLast();

    /**
     * @return An iterator over elements in this deque in tail-to-head order. 
     */
    public Iterator<ShortCursor> descendingIterator();
    
    /**
     * Applies a <code>procedure</code> to all container elements.
     */
    public <T extends ShortProcedure> T descendingForEach(T procedure);

    /**
     * Applies a <code>predicate</code> to container elements as long, as the predicate
     * returns <code>true</code>. The iteration is interrupted otherwise. 
     */
    public <T extends ShortPredicate> T descendingForEach(T predicate);

    /**
     * Compares the specified object with this deque for equality. Returns
     * <tt>true</tt> if and only if the specified object is also a
     * {@link ObjectDeque}, and all corresponding
     * pairs of elements acquired from forward iterators are the same. In other words, two indexed
     * containers are defined to be equal if they contain the same elements in the same
     * order of iteration.
     * <p>
     * Note that, unlike in {@link List}, deques may be of different types and still
     * return <code>true</code> from {@link #equals}. This may be dangerous if you use
     * different hash functions in two containers, but don't override the default 
     * implementation of {@link #equals}. It is the programmer's responsibility to 
     * enforcing these contracts properly.
     * </p>
     */
    public boolean equals(Object obj);

    /**
     * @return A hash code of elements stored in the deque. The hash code
     * is defined identically to {@link List#hashCode()} (should be implemented
     * with the same algorithm), replacing forward index loop with a forward iterator
     * loop.
     */
    public int hashCode();
}
