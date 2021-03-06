package objsets

import java.util.NoSuchElementException

import TweetReader._

/**
 * A class to represent tweets.
 */
class Tweet(val user: String, val text: String, val retweets: Int) {
  override def toString: String =
    "User: " + user + "\n" +
    "Text: " + text + " [" + retweets + "]"
}

/**
 * This represents a set of objects of type `Tweet` in the form of a binary search
 * tree. Every branch in the tree has two children (two `TweetSet`s). There is an
 * invariant which always holds: for every branch `b`, all elements in the left
 * subtree are smaller than the tweet at `b`. The elements in the right subtree are
 * larger.
 *
 * Note that the above structure requires us to be able to compare two tweets (we
 * need to be able to say which of two tweets is larger, or if they are equal). In
 * this implementation, the equality / order of tweets is based on the tweet's text
 * (see `def incl`). Hence, a `TweetSet` could not contain two tweets with the same
 * text from different users.
 *
 *
 * The advantage of representing sets as binary search trees is that the elements
 * of the set can be found quickly. If you want to learn more you can take a look
 * at the Wikipedia page [1], but this is not necessary in order to solve this
 * assignment.
 *
 * [1] http://en.wikipedia.org/wiki/Binary_search_tree
 */
abstract class TweetSet {

  /**
   * This method takes a predicate and returns a subset of all the elements
   * in the original set for which the predicate is true.
   *
   * Question: Can we implment this method here, or should it remain abstract
   * and be implemented in the subclasses?
   */
    def filter(p: Tweet => Boolean): TweetSet = filterAcc(p, new EmptySet)

  /**
   * This is a helper method for `filter` that propagetes the accumulated tweets.
   */
  def filterAcc(p: Tweet => Boolean, acc: TweetSet): TweetSet

  /**
   * Returns a new `TweetSet` that is the union of `TweetSet`s `this` and `that`.
   *
   * Question: Should we implment this method here, or should it remain abstract
   * and be implemented in the subclasses?
   */
    def union(that: TweetSet): TweetSet

  /**
   * Returns the tweet from this set which has the greatest retweet count.
   *
   * Calling `mostRetweeted` on an empty set should throw an exception of
   * type `java.util.NoSuchElementException`.
   *
   * Question: Should we implment this method here, or should it remain abstract
   * and be implemented in the subclasses?
   */
    def mostRetweeted: Tweet

  /**
   * Returns a list containing all tweets of this set, sorted by retweet count
   * in descending order. In other words, the head of the resulting list should
   * have the highest retweet count.
   *
   * Hint: the method `remove` on TweetSet will be very useful.
   * Question: Should we implment this method here, or should it remain abstract
   * and be implemented in the subclasses?
   */
    def descendingByRetweet: TweetList

  /**
   * The following methods are already implemented
   */

  /**
   * Returns a new `TweetSet` which contains all elements of this set, and the
   * the new element `tweet` in case it does not already exist in this set.
   *
   * If `this.contains(tweet)`, the current set is returned.
   */
  def include(tweet: Tweet): TweetSet

  /**
   * Returns a new `TweetSet` which excludes `tweet`.
   */
  def remove(tweet: Tweet): TweetSet

  /**
   * Tests if `tweet` exists in this `TweetSet`.
   */
  def contains(tweet: Tweet): Boolean

  /**
   * This method takes a function and applies it to every element in the set.
   */
  def foreach(f: Tweet => Unit): Unit

  def isEmpty : Boolean

}

class EmptySet extends TweetSet {
    def filterAcc(p: Tweet => Boolean, acc: TweetSet): TweetSet = new EmptySet

  /**
   * The following methods are already implemented
   */

  def contains(tweet: Tweet): Boolean = false

  def include(tweet: Tweet): TweetSet = new OrderedSet(tweet, new EmptySet, new EmptySet)

  def remove(tweet: Tweet): TweetSet = this

  def foreach(f: Tweet => Unit): Unit = ()

  def isEmpty = true
  def leftestNode = throw new Exception("Empty.head")
  def followingNode = throw new Exception("Empty.tail")

  override def union(that: TweetSet): TweetSet = that

  override def descendingByRetweet: TweetList = Nil

  override def mostRetweeted: Tweet = throw new NoSuchElementException
}

class OrderedSet(elem: Tweet, left: TweetSet, right: TweetSet) extends TweetSet {

    def filterAcc(predicate: Tweet => Boolean, accumulator: TweetSet): TweetSet = {
      val searchLeftRight = left.filterAcc(predicate, accumulator) union right.filterAcc(predicate, accumulator)
      if(predicate(elem)) searchLeftRight.include(elem)
      else searchLeftRight
    }

  /**
   * The following methods are already implemented
   */

  def contains(x: Tweet): Boolean =
    if (x.text < elem.text) left.contains(x)
    else if (elem.text < x.text) right.contains(x)
    else true

  def include(tweetToAdd: Tweet): TweetSet = {
    if (tweetToAdd.text < elem.text) new OrderedSet(elem, left.include(tweetToAdd), right)
    else if (elem.text < tweetToAdd.text) new OrderedSet(elem, left, right.include(tweetToAdd))
    else this
  }

  def remove(tw: Tweet): TweetSet =
    if (tw.text < elem.text) new OrderedSet(elem, left.remove(tw), right)
    else if (elem.text < tw.text) new OrderedSet(elem, left, right.remove(tw))
    else left.union(right)

  def foreach(f: Tweet => Unit): Unit = {
    f(elem)
    left.foreach(f)
    right.foreach(f)
  }

  override def isEmpty = false

  override def union(that: TweetSet): TweetSet = {
    val x= left.union(right.union(that.include(elem)))
    x
  }

  override def descendingByRetweet: TweetList = {
    def loop(set: TweetSet, list: TweetList): TweetList = {
      if (set.isEmpty) list
      else {
        val mostPopularTweet = set.mostRetweeted
        new Cons(mostPopularTweet, loop(set.remove(mostPopularTweet), list))
      }
    }
    loop(this, Nil)
  }

  override def mostRetweeted: Tweet = {
    val allNodes: TweetSet = left.union(right)
    val nodeWithHighestRetweets: TweetSet = allNodes.filter(givenNode => givenNode.retweets > elem.retweets)
    if(nodeWithHighestRetweets.isEmpty) elem
    else nodeWithHighestRetweets.mostRetweeted
  }
}

trait TweetList {
  def head: Tweet
  def tail: TweetList
  def isEmpty: Boolean
  def foreach(f: Tweet => Unit): Unit =
    if (!isEmpty) {
      f(head)
      tail.foreach(f)
    }
}

object Nil extends TweetList {
  def head = throw new java.util.NoSuchElementException("head of EmptyList")
  def tail = throw new java.util.NoSuchElementException("tail of EmptyList")
  def isEmpty = true
}

class Cons(val head: Tweet, val tail: TweetList) extends TweetList {
  def isEmpty = false
}


object GoogleVsApple {
  val google = List("android", "Android", "galaxy", "Galaxy", "nexus", "Nexus")
  val apple = List("ios", "iOS", "iphone", "iPhone", "ipad", "iPad")

    lazy val googleTweets: TweetSet = ???
  lazy val appleTweets: TweetSet = ???
  
  /**
   * A list of all tweets mentioning a keyword from either apple or google,
   * sorted by the number of retweets.
   */
     lazy val trending: TweetList = ???
  }

object Main extends App {
  // Print the trending tweets
  GoogleVsApple.trending foreach println
}
