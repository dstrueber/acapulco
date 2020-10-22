package com.carrotsearch.hppc;

import java.util.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.hash.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;

import static com.carrotsearch.hppc.Internals.*;
import static com.carrotsearch.hppc.HashContainerUtils.*;

/**
 * A hash set of <code>byte</code>s, implemented using using open
 * addressing with linear probing for collision resolution.
 * 
 * <p>
 * The internal buffers of this implementation ({@link #keys}), {@link #allocated})
 * are always allocated to the nearest size that is a power of two. When
 * the capacity exceeds the given load factor, the buffer size is doubled.
 * </p>
 * <p>See {@link ObjectOpenHashSet} class for API similarities and differences against Java
 * Collections.  
 * 
 * <p><b>Important node.</b> The implementation uses power-of-two tables and linear
 * probing, which may cause poor performance (many collisions) if hash values are
 * not properly distributed. 
 * This implementation uses {@link MurmurHash3} for rehashing keys.</p>
 * 
 * @author This code is inspired by the collaboration and implementation in the <a
 *         href="http://fastutil.dsi.unimi.it/">fastutil</a> project.
 */
 @javax.annotation.Generated(date = "2014-09-08T10:42:29+0200", value = "HPPC generated from: ByteOpenHashSet.java") 
public class ByteOpenHashSet
    extends AbstractByteCollection 
    implements ByteLookupContainer, ByteSet, Cloneable
{
    /**
     * Minimum capacity for the map.
     */
    public final static int MIN_CAPACITY = HashContainerUtils.MIN_CAPACITY;

    /**
     * Default capacity.
     */
    public final static int DEFAULT_CAPACITY = HashContainerUtils.DEFAULT_CAPACITY;

    /**
     * Default load factor.
     */
    public final static float DEFAULT_LOAD_FACTOR = HashContainerUtils.DEFAULT_LOAD_FACTOR;

    /**
     * Hash-indexed array holding all set entries.
     * 
     * 
     * @see #allocated
     */
    public byte [] keys;

    /**
     * Information if an entry (slot) in the {@link #keys} table is allocated
     * or empty.
     * 
     * @see #assigned
     */
    public boolean [] allocated;

    /**
     * Cached number of assigned slots in {@link #allocated}.
     */
    public int assigned;

    /**
     * The load factor for this map (fraction of allocated slots
     * before the buffers must be rehashed or reallocated).
     */
    public final float loadFactor;

    /**
     * Resize buffers when {@link #allocated} hits this value. 
     */
    protected int resizeAt;

    /**
     * The most recent slot accessed in {@link #contains}.
     * 
     * @see #contains
     *      */
    protected int lastSlot;
    
    /**
     * We perturb hashed values with the array size to avoid problems with
     * nearly-sorted-by-hash values on iterations.
     * 
     * @see "http://issues.carrot2.org/browse/HPPC-80"
     */
    protected int perturbation;

    /**
     * Creates a hash set with the default capacity of {@value #DEFAULT_CAPACITY},
     * load factor of {@value #DEFAULT_LOAD_FACTOR}.
`     */
    public ByteOpenHashSet()
    {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a hash set with the given capacity,
     * load factor of {@value #DEFAULT_LOAD_FACTOR}.
     */
    public ByteOpenHashSet(int initialCapacity)
    {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a hash set with the given capacity and load factor.
     */
    public ByteOpenHashSet(int initialCapacity, float loadFactor)
    {
        initialCapacity = Math.max(initialCapacity, MIN_CAPACITY);

        assert initialCapacity > 0
            : "Initial capacity must be between (0, " + Integer.MAX_VALUE + "].";
        assert loadFactor > 0 && loadFactor <= 1
            : "Load factor must be between (0, 1].";

        this.loadFactor = loadFactor;
        allocateBuffers(roundCapacity(initialCapacity));
    }

    /**
     * Creates a hash set from elements of another container. Default load factor is used.
     */
    public ByteOpenHashSet(ByteContainer container)
    {
        this((int) (container.size() * (1 + DEFAULT_LOAD_FACTOR)));
        addAll(container);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(byte e)
    {
        assert assigned < allocated.length;

        final int mask = allocated.length - 1;
        int slot = rehash(e, perturbation) & mask;
        while (allocated[slot])
        {
            if (((e) == (keys[slot])))
            {
                return false;
            }

            slot = (slot + 1) & mask;
        }

        // Check if we need to grow. If so, reallocate new data, 
        // fill in the last element and rehash.
        if (assigned == resizeAt) {
            expandAndAdd(e, slot);
        } else {
            assigned++;
            allocated[slot] = true;
            keys[slot] = e;                
        }
        return true;
    }

    /**
     * Adds two elements to the set.
     */
    public int add(byte e1, byte e2)
    {
        int count = 0;
        if (add(e1)) count++;
        if (add(e2)) count++;
        return count;
    }

    /**
     * Vararg-signature method for adding elements to this set.
     * <p><b>This method is handy, but costly if used in tight loops (anonymous 
     * array passing)</b></p>
     * 
     * @return Returns the number of elements that were added to the set
     * (were not present in the set).
     */
    public int add(byte... elements)
    {
        int count = 0;
        for (byte e : elements)
            if (add(e)) count++;
        return count;
    }

    /**
     * Adds all elements from a given container to this set.
     * 
     * @return Returns the number of elements actually added as a result of this
     * call (not previously present in the set).
     */
    public int addAll(ByteContainer container)
    {
        return addAll((Iterable<? extends ByteCursor>) container);
    }

    /**
     * Adds all elements from a given iterable to this set.
     * 
     * @return Returns the number of elements actually added as a result of this
     * call (not previously present in the set).
     */
    public int addAll(Iterable<? extends ByteCursor> iterable)
    {
        int count = 0;
        for (ByteCursor cursor : iterable)
        {
            if (add(cursor.value)) count++;
        }
        return count;
    }

    /**
     * Expand the internal storage buffers (capacity) or rehash current
     * keys and values if there are a lot of deleted slots.
     */
    private void expandAndAdd(byte pendingKey, int freeSlot)
    {
        assert assigned == resizeAt;
        assert !allocated[freeSlot];

        // Try to allocate new buffers first. If we OOM, it'll be now without
        // leaving the data structure in an inconsistent state.
        final byte   [] oldKeys      = this.keys;
        final boolean [] oldAllocated = this.allocated;

        allocateBuffers(nextCapacity(keys.length));

        // We have succeeded at allocating new data so insert the pending key/value at
        // the free slot in the old arrays before rehashing.
        lastSlot = -1;
        assigned++;
        oldAllocated[freeSlot] = true;
        oldKeys[freeSlot] = pendingKey;
        
        // Rehash all stored keys into the new buffers.
        final byte []   keys = this.keys;
        final boolean [] allocated = this.allocated;
        final int mask = allocated.length - 1;
        for (int i = oldAllocated.length; --i >= 0;)
        {
            if (oldAllocated[i])
            {
                final byte k = oldKeys[i];

                int slot = rehash(k, perturbation) & mask;
                while (allocated[slot])
                {
                    slot = (slot + 1) & mask;
                }

                allocated[slot] = true;
                keys[slot] = k;                
            }
        }

        /*  */
    }


    /**
     * Allocate internal buffers for a given capacity.
     * 
     * @param capacity New capacity (must be a power of two).
     */
    private void allocateBuffers(int capacity)
    {
        byte [] keys = new byte [capacity];
        boolean [] allocated = new boolean [capacity];

        this.keys = keys;
        this.allocated = allocated;

        this.resizeAt = Math.max(2, (int) Math.ceil(capacity * loadFactor)) - 1;
        this.perturbation = computePerturbationValue(capacity);
    }

    /**
     * <p>Compute the key perturbation value applied before hashing. The returned value
     * should be non-zero and ideally different for each capacity. This matters because
     * keys are nearly-ordered by their hashed values so when adding one container's
     * values to the other, the number of collisions can skyrocket into the worst case
     * possible.
     * 
     * <p>If it is known that hash containers will not be added to each other 
     * (will be used for counting only, for example) then some speed can be gained by 
     * not perturbing keys before hashing and returning a value of zero for all possible
     * capacities. The speed gain is a result of faster rehash operation (keys are mostly
     * in order).   
     */
    protected int computePerturbationValue(int capacity)
    {
        return PERTURBATIONS[Integer.numberOfLeadingZeros(capacity)];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAllOccurrences(byte key)
    {
        return remove(key) ? 1 : 0;
    }

    /**
     * An alias for the (preferred) {@link #removeAllOccurrences}.
     */
    public boolean remove(byte key)
    {
        final int mask = allocated.length - 1;
        int slot = rehash(key, perturbation) & mask; 

        while (allocated[slot])
        {
            if (((key) == (keys[slot])))
             {
                assigned--;
                shiftConflictingKeys(slot);
                return true;
             }
             slot = (slot + 1) & mask;
        }

        return false;
    }

    /**
     * Shift all the slot-conflicting keys allocated to (and including) <code>slot</code>. 
     */
    protected void shiftConflictingKeys(int slotCurr)
    {
        // Copied nearly verbatim from fastutil's impl.
        final int mask = allocated.length - 1;
        int slotPrev, slotOther;
        while (true)
        {
            slotCurr = ((slotPrev = slotCurr) + 1) & mask;

            while (allocated[slotCurr])
            {
                slotOther = rehash(keys[slotCurr], perturbation) & mask;
                if (slotPrev <= slotCurr)
                {
                    // We are on the right of the original slot.
                    if (slotPrev >= slotOther || slotOther > slotCurr)
                        break;
                }
                else
                {
                    // We have wrapped around.
                    if (slotPrev >= slotOther && slotOther > slotCurr)
                        break;
                }
                slotCurr = (slotCurr + 1) & mask;
            }

            if (!allocated[slotCurr]) 
                break;

            // Shift key/value pair.
            keys[slotPrev] = keys[slotCurr];
        }

        allocated[slotPrev] = false;

        /*  */
    }

    /*  */

    /**
     * @return Returns the slot of the last key looked up in a call to {@link #contains} if
     * it returned <code>true</code>.
     * 
     * @see #contains
     */
    public int lslot()
    {
        assert lastSlot >= 0 : "Call contains() first.";
        return lastSlot;
    }

    /**
     * {@inheritDoc}
     * 
     *      */
    @Override
    public boolean contains(byte key)
    {
        final int mask = allocated.length - 1;
        int slot = rehash(key, perturbation) & mask;
        while (allocated[slot])
        {
            if (((key) == (keys[slot])))
            {
                lastSlot = slot;
                return true; 
            }
            slot = (slot + 1) & mask;
        }
        lastSlot = -1;
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Does not release internal buffers.</p>
     */
    @Override
    public void clear()
    {
        assigned = 0;

        Arrays.fill(allocated, false);
        Arrays.fill(keys, ((byte) 0));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return assigned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        return size() == 0;
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public int hashCode()
    {
        int h = 0;

        final byte [] keys = this.keys;
        final boolean [] states = this.allocated;
        for (int i = states.length; --i >= 0;)
        {
            if (states[i])
            {
                h += rehash(keys[i]);
            }
        }

        return h;
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    /*  */
    public boolean equals(Object obj)
    {
        if (obj != null)
        {
            if (obj == this) return true;

            if (obj instanceof ByteSet)
            {
                ByteSet other = (ByteSet) obj;
                if (other.size() == this.size())
                {
                    for (ByteCursor c : this)
                    {
                        if (!other.contains(c.value))
                        {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * An iterator implementation for {@link #iterator}.
     */
    private final class EntryIterator extends AbstractIterator<ByteCursor>
    {
        private final ByteCursor cursor;

        public EntryIterator()
        {
            cursor = new ByteCursor();
            cursor.index = -1;
        }

        @Override
        protected ByteCursor fetch()
        {
            final int max = keys.length;

            int i = cursor.index + 1;
            while (i < keys.length && !allocated[i])
            {
                i++;
            }

            if (i == max)
                return done();

            cursor.index = i;
            cursor.value = keys[i];
            return cursor;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<ByteCursor> iterator()
    {
        return new EntryIterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends ByteProcedure> T forEach(T procedure)
    {
        final byte [] keys = this.keys;
        final boolean [] states = this.allocated;

        for (int i = 0; i < states.length; i++)
        {
            if (states[i])
                procedure.apply(keys[i]);
        }

        return procedure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
         public byte [] toArray()
         
    {
        final byte [] cloned = new byte [assigned];
        for (int i = 0, j = 0; i < keys.length; i++)
            if (allocated[i])
                cloned[j++] = keys[i];
        return cloned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteOpenHashSet clone()
    {
        try
        {
            /*  */
            ByteOpenHashSet cloned = (ByteOpenHashSet) super.clone();
            cloned.keys = keys.clone();
            cloned.allocated = allocated.clone();
            return cloned;
        }
        catch (CloneNotSupportedException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends BytePredicate> T forEach(T predicate)
    {
        final byte [] keys = this.keys;
        final boolean [] states = this.allocated;

        for (int i = 0; i < states.length; i++)
        {
            if (states[i])
            {
                if (!predicate.apply(keys[i]))
                    break;
            }
        }

        return predicate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(BytePredicate predicate)
    {
        final byte [] keys = this.keys;
        final boolean [] allocated = this.allocated;

        int before = assigned;
        for (int i = 0; i < allocated.length;)
        {
            if (allocated[i])
            {
                if (predicate.apply(keys[i]))
                {
                    assigned--;
                    shiftConflictingKeys(i);
                    // Repeat the check for the same i.
                    continue;
                }
            }
            i++;
        }

        return before - this.assigned;
    }

    /**
     * Create a set from a variable number of arguments or an array of <code>byte</code>.
     * The elements are copied from the argument to the internal buffer.
     */
    public static  ByteOpenHashSet from(byte... elements)
    {
        final ByteOpenHashSet set = new ByteOpenHashSet(
            (int) (elements.length * (1 + DEFAULT_LOAD_FACTOR)));
        set.add(elements);
        return set;
    }

    /**
     * Create a set from elements of another container.
     */
    public static  ByteOpenHashSet from(ByteContainer container)
    {
        return new ByteOpenHashSet(container);
    }
    
    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static  ByteOpenHashSet newInstance()
    {
        return new ByteOpenHashSet();
    }

    /**
     * Returns a new object with no key perturbations (see
     * {@link #computePerturbationValue(int)}). Only use when sure the container will not
     * be used for direct copying of keys to another hash container.
     */
    public static  ByteOpenHashSet newInstanceWithoutPerturbations()
    {
        return new ByteOpenHashSet() {
            @Override
            protected int computePerturbationValue(int capacity) { return 0; }
        };
    }

    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static  ByteOpenHashSet newInstanceWithCapacity(int initialCapacity, float loadFactor)
    {
        return new ByteOpenHashSet(initialCapacity, loadFactor);
    }

    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor). The returned instance will have enough initial
     * capacity to hold <code>expectedSize</code> elements without having to resize.
     */
    public static  ByteOpenHashSet newInstanceWithExpectedSize(int expectedSize)
    {
        return newInstanceWithExpectedSize(expectedSize, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor). The returned instance will have enough initial
     * capacity to hold <code>expectedSize</code> elements without having to resize.
     */
    public static  ByteOpenHashSet newInstanceWithExpectedSize(int expectedSize, float loadFactor)
    {
        return newInstanceWithCapacity((int) (expectedSize / loadFactor) + 1, loadFactor);
    }
}
