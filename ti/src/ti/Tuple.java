// Copyright (C) 2015  Julián Urbano <urbano.julian@gmail.com>
// Distributed under the terms of the MIT License.

package ti;

/**
 * This class represents a tuple of two items. *
 * @param <T1> The type of the first item in the tuple.
 * @param <T2> the type of the second item in the tuple.
 */
public class Tuple<T1, T2>
{
    /**
     * The first item in the tuple.
     */
    public T1 item1;
    /**
     * The second item in the tuple.
     */
    public T2 item2;

    /**
     * Creates a new tuple with the two given items.
     *
     * @param item1 The first item in the tuple.
     * @param item2 The second item in the tuple.
     */
    public Tuple(T1 item1, T2 item2)
    {
        this.item1 = item1;
        this.item2 = item2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "(" + this.item1.toString() + ", " + this.item2.toString() + ")";
    }
}