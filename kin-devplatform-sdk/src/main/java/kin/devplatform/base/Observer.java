package kin.devplatform.base;

public abstract class Observer<T> {

	public abstract void onChanged(T value);
}
