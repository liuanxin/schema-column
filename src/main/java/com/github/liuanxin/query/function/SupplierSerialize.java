package com.github.liuanxin.query.function;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * <pre>
 * public static &lt;T&gt; void supp(SupplierSerialize&lt;T&gt; supplier) {
 * }
 *
 * class Example {
 *     private Long id;
 *
 *     public Long getId() { return id; }
 *     public void setId(Long id) { this.id = id; }
 * }
 *
 * public void main(String[] args) {
 *     Example example = new Example();
 *     supp(example::getId);
 *     // supp(Example::getId); // compile error : <span style="color:red">Non-static method cannot be referenced from a static context</span>
 * }
 * </pre>
 */
public interface SupplierSerialize<T> extends Supplier<T>, Serializable {
}
