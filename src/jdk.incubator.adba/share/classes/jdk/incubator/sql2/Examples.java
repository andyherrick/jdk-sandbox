/*
 * Copyright (c)  2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.incubator.sql2;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.Semaphore;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Simple example code using various aspects of ADBA. These do not necessarily
 * demonstrate the best way to use each feature, just one way.
 */
public class Examples {
  
  // DataSourceFactory

  public DataSource getDataSource() {
    return DataSourceFactory.newFactory("oracle.database.adba")
            .builder()
            .url("//host.oracle.com:5521/example")
            .username("scott")
            .password("tiger")
            .build();
  }
  
  // RowCountOperation
  
  public void insertItem(DataSource ds, Item item) {
    try (Session session = ds.getSession()) {
      session.rowCountOperation("insert into tab values (:id, :name, :answer)")
              .set("id", item.id(), AdbaType.NUMERIC)
              .set("name", item.name(), AdbaType.VARCHAR)
              .set("answer", item.answer(), AdbaType.NUMERIC)
              .submit();
    }
  }
  
  // RowOperation
  
  public void idsForAnswer(DataSource ds, List<Integer> result, int correctAnswer) {
    try (Session session = ds.getSession()) {
      session.<List<Integer>>rowOperation("select id, name, answer from tab where answer = :target")
              .set("target", correctAnswer, AdbaType.NUMERIC)
              .collect(() -> result, 
                       (list, row) -> list.add(row.at("id").get(Integer.class)) )
              .submit();
    }
  }
  
  // RowOperation
  
  public CompletionStage<List<Item>> itemsForAnswer(DataSource ds, int answer) {
    try (Session session = ds.getSession()) {
      return session.<List<Item>>rowOperation("select id, name, answer from tab where answer = :target")
              .set("target", 42, AdbaType.NUMERIC)
              .collect(Collectors.mapping( 
                       row -> new Item(row.at("id").get(Integer.class),
                                       row.at("name").get(String.class),
                                       row.at("answer").get(Integer.class) ),
                       Collectors.toList() ))
              .submit()
              .getCompletionStage();
    }
  }
  
  // Independent OperationGroup

  public void insertItemsIndependent(DataSource ds, List<Item> list) {
    String sql = "insert into tab values (:id, :name, :answer)";
    try (Session session = ds.getSession()) {
      OperationGroup group = session.operationGroup()
              .independent();
      for (Item elem : list) {
        group.rowCountOperation(sql)
                .set("id", elem.id)
                .set("name", elem.name)
                .set("answer", elem.answer)
                .submit()
                .getCompletionStage()
                .exceptionally( t -> {
                  System.out.println(elem.id);
                  return null;
                });
      }
      group.submit();
      
    }
  }
  
  // Held OperationGroup
  
  public void insertItemsHold(DataSource ds, List<Item> list) {
    String sql = "insert into tabone values (:id, :name, :answer)";
    try (Session session = ds.getSession()) {
      OperationGroup group = session.operationGroup()
              .independent();
      group.submitHoldingForMoreMembers();
      for (Item elem : list) {
        group.rowCountOperation(sql)
                .set("elem_", elem)
                .submit()
                .getCompletionStage()
                .exceptionally( t -> {
                  System.out.println(elem.id);
                  return null;
                });
      }
      group.releaseProhibitingMoreMembers();
    }
  }
  
  // Parallel, Independent OperationGroup
  
  public void updateListParallel(List<Item> list, DataSource ds) {
    String query = "select id from tab where answer = :answer";
    String update = "update tab set name = :name where id = :id";
    try (Session session = ds.getSession()) {
      OperationGroup<Object, Object> group = session.operationGroup()
              .independent()
              .parallel();
      group.submitHoldingForMoreMembers();
      for (Item elem : list) {
        CompletionStage<Integer> idPromise = group.<List<Integer>>rowOperation(query)
                .set("answer", elem.answer, AdbaType.NUMERIC)
                .collect( Collector.of(
                        () -> new ArrayList<>(),
                        (l, row) -> l.add( row.at("id").get(Integer.class) ),
                        (l, r) -> l ))
                .submit()
                .getCompletionStage()
                .thenApply( l -> l.get(0) );
        group.rowCountOperation(update)
                .set("id", idPromise)
                .set("name", "the ultimate question")
                .submit()
                .getCompletionStage()
                .exceptionally( t -> {
                  System.out.println(elem.id);
                  return null;
                });
      }
      group.releaseProhibitingMoreMembers();
    }
  }
  
  // TransactionEnd
  
  public void transaction(DataSource ds) {
    try (Session session = ds.getSession(t -> System.out.println("ERROR: " + t.toString()))) {
      TransactionEnd trans = session.transactionEnd();
      CompletionStage<Integer> idPromise = session.<Integer>rowOperation("select empno, ename from emp where ename = :1 for update")
              .set("1", "CLARK", AdbaType.VARCHAR)
              .collect(Collectors.collectingAndThen(
                                        Collectors.mapping(r -> r.at("empno").get(Integer.class), 
                                                           Collectors.toList()), 
                                        l -> l.get(0)))
              .onError( t -> trans.setRollbackOnly() )
              .submit()
              .getCompletionStage();
      session.<Long>rowCountOperation("update emp set deptno = :1 where empno = :2")
              .set("1", 50, AdbaType.INTEGER)
              .set("2", idPromise, AdbaType.INTEGER)
              .apply(c -> { 
                if (c.getCount() != 1L) {
                  trans.setRollbackOnly();
                  throw new RuntimeException("updated wrong number of rows");
                }
                return c.getCount();
              })
              .onError(t -> trans.setRollbackOnly() )
              .submit();
          //    .getCompletionStage()
          //    .exceptionally( t -> { trans.setRollbackOnly(); return null; } ) // incorrect
      session.catchErrors();
      session.commitMaybeRollback(trans);
    }    
  }
  
  // RowPublisherOperation
  
  public CompletionStage<List<String>> rowSubscriber(DataSource ds) {
    
    String sql = "select empno, ename from emp";
    CompletableFuture<List<String>> result = new CompletableFuture<>();

    Flow.Subscriber<Result.RowColumn> subscriber = new Flow.Subscriber<>() {

      Flow.Subscription subscription;
      List<String> names = new ArrayList<>();
      int demand = 0;

      @Override
      public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        this.subscription.request(10);
        demand += 10;
      }

      @Override
      public void onNext(Result.RowColumn column) {
        names.add(column.at("ename").get(String.class));
        if (--demand < 1) {
          subscription.request(10);
          demand += 10;
        }
      }

      @Override
      public void onError(Throwable throwable) {
        result.completeExceptionally(throwable);
      }

      @Override
      public void onComplete() {
        result.complete(names);
      }

    };
    
    try (Session session = ds.getSession()) {
      return session.<List<String>>rowPublisherOperation(sql)
              .subscribe(subscriber, result)
              .submit()
              .getCompletionStage();
    }
  }
  
  // Control Operation Submission Rate
    
  public CompletionStage<Long> insertRecords(DataSource ds, DataInputStream in) {
    String insert = "insert into tab values (@record)";
    try (Session session = ds.getSession()) {
      OperationGroup<Long, Long> group = session.<Long, Long>operationGroup()
              .independent()
              .collect(Collectors.summingLong(c -> c));
      group.submitHoldingForMoreMembers();
      Semaphore demand = new Semaphore(0);
      session.requestHook( n -> demand.release((int)n) );
      while (in.available() > 0) {
        demand.acquire(1); // user thread blocked by Semaphore, not by ADBA
        group.<Long>rowCountOperation(insert)
                    .set("record", in.readUTF(), AdbaType.VARCHAR)
                    .apply(c -> c.getCount())
                    .submit();
      }
      return group.releaseProhibitingMoreMembers()
                  .getCompletionStage();
    }
    catch (IOException | InterruptedException ex) { 
      throw new SqlException(ex);
    }
  }  
  
  // ArrayRowCountOperation
  
  public CompletionStage<Long> arrayInsert(DataSource ds, 
                                           List<Integer> ids, 
                                           List<String> names, 
                                           List<Integer> answers) {
    String sql = "insert into tab values (?, ?, ?)";
    try (Session session = ds.getSession()) {
      return session.<Long>arrayRowCountOperation(sql)
          .collect(Collectors.summingLong( c -> c.getCount() ))
          .set("1",ids, AdbaType.INTEGER)
          .set("2", names, AdbaType.VARCHAR)
          .set("3", answers, AdbaType.INTEGER)
          .submit()
          .getCompletionStage();
    }
  }
  
  // ArrayRowCountOperation -- transposed
  
  public CompletionStage<Long> transposedArrayInsert(DataSource ds, List<Item> items) {
    String sql = "insert into tab values (?, ?, ?)";
    try (Session session = ds.getSession()) {
      return session.<Long>arrayRowCountOperation(sql)
          .collect(Collectors.summingLong( c -> c.getCount() ))
          .set("1", items.stream().map(Item::id).collect(Collectors.toList()), AdbaType.INTEGER)
          .set("2", items.stream().map(Item::name).collect(Collectors.toList()), AdbaType.VARCHAR)
          .set("3", items.stream().map(Item::answer).collect(Collectors.toList()), AdbaType.INTEGER)
          .submit()
          .getCompletionStage();
    }
  }
  
  // OutOperation
  
  public CompletionStage<Item> getItem(DataSource ds, int id) {
    String sql = "call item_for_id(:id, :name, :answer)";
    try (Session session = ds.getSession()) {
      return session.<Item>outOperation(sql)
              .set("id", id, AdbaType.INTEGER)
              .outParameter("name", AdbaType.VARCHAR)
              .outParameter("answer", AdbaType.INTEGER)
              .apply( out -> new Item(id, 
                                      out.at("name").get(String.class), 
                                      out.at("answer").get(Integer.class)) )
              .submit()
              .getCompletionStage();
    }
  }
  
  // MultiOperation
  
  // LocalOperation
  
  // SessionProperty
  
  // Sharding
  
  // TransactionOutcome
  
  // Column navigation
  
  private class Name { Name(String ... args) {} }
  private class Address { Address(String ... args) {} }
  
  private Name getName(Result.Column col) {
    return new Name(
      col.get(String.class), // title
      col.next().get(String.class), // given name
      col.next().get(String.class), // middle initial
      col.next().get(String.class), // family name
      col.next().get(String.class)); // suffix
  }
  
  private Address getAddress(Result.Column col) {
    List<String> a = new ArrayList<>();
    for (Result.Column c : col.slice(6)) {
      a.add(c.get(String.class));
    }
    return new Address(a.toArray(new String[0]));
  }
  public void columNavigation(Result.RowColumn column) {
    Name fullName = getName(column.at("name_title"));
    Address streetAddress = getAddress(column.at("street_address_line1"));
    Address mailingAddress = getAddress(column.at("mailing_address_line1"));
    for (Result.Column c : column.at(-14)) { // dump the last 14 columns
      System.out.println("trailing column " + c.get(String.class));
    }
  }
  
  // Error handling

  
  static public class Item {
    public int id;
    public String name;
    public int answer;
    
    public Item(int i, String n, int a) {
      id =i;
      name = n;
      answer = a;
    }
    
    public int id() {
      return id;
    }
    
    public String name() {
      return name;
    }
    
    public int answer() {
      return answer;
    }
  }

}
