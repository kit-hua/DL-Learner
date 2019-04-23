package org.dllearner.ray;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class SetOperations implements java.io.Serializable{
	
	public static <T> Set<T> copy (Collection<T> original){
		Set<T> cp = original.stream().collect(Collectors.toSet());
		return cp;
	}

	public static <T> Set<T> intersection (Set<T> one, Set<T> other){
		Set<T> cp = copy(one);
		cp.retainAll(other);
		return cp;
	}
	
	public static <T> Set<T> union (Set<T> one, Set<T> other){
		Set<T> cp = copy(one);
		cp.addAll(other);
		return cp;
	}
	
	public static <T> Set<T> difference (Set<T> one, Set<T> other){
		Set<T> cp = copy(one);
		cp.removeAll(other);
		return cp;
	}
}
