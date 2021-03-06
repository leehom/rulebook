package com.deliveredtechnologies.rulebook.model.runner;

import com.deliveredtechnologies.rulebook.Fact;
import com.deliveredtechnologies.rulebook.FactMap;
import com.deliveredtechnologies.rulebook.model.Rule;
import com.deliveredtechnologies.rulebook.model.RuleBook;
import net.jodah.concurrentunit.Waiter;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RuleBookRunner}.
 */
public class RuleBookRunnerTest {
  @Test
  public void ruleBookRunnerShouldAddRuleClassesInPackage() {
    RuleBookRunner ruleBookRunner = new RuleBookRunner("com.deliveredtechnologies.rulebook.runner");
    ruleBookRunner.run(new FactMap());

    Assert.assertTrue(ruleBookRunner.hasRules());
  }

  @Test
  public void ruleBookRunnerShouldNotLoadClassesIfNotInPackage() {
    RuleBookRunner ruleBookRunner = new RuleBookRunner("com.deliveredtechnologies.rulebook");
    ruleBookRunner.run(new FactMap());

    Assert.assertFalse(ruleBookRunner.hasRules());
  }

  @Test
  public void ruleBookRunnerShouldNotLoadClassesForInvalidPackage() {
    RuleBookRunner ruleBookRunner = new RuleBookRunner("com.deliveredtechnologies.rulebook.invalid");
    ruleBookRunner.run(new FactMap());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void ruleBookRunnerOrdersTheExecutionOfRules() {
    Fact<String> fact1 = new Fact("fact1", "Fact");
    Fact<String> fact2 = new Fact("fact2", "Fact");
    FactMap<String> factMap = new FactMap<>();

    RuleBookRunner ruleBookRunner = new RuleBookRunner("com.deliveredtechnologies.rulebook.runner");
    factMap.put(fact1);
    factMap.put(fact2);
    ruleBookRunner.run(factMap);

    Assert.assertEquals("So Factual Too!", fact1.getValue());
    Assert.assertEquals("So Factual!", fact2.getValue());
    Assert.assertEquals("Equivalence, Bitches!", ruleBookRunner.getResult().get().toString());
  }

  @Test(expected = IllegalStateException.class)
  public void rulesCanNotBeAddedByCallingAddRule() {
    Rule rule = mock(Rule.class);
    RuleBookRunner ruleBookRunner = new RuleBookRunner("com.deliveredtechnologies.rulebook.runner");
    ruleBookRunner.addRule(rule);
  }

  @Test
  public void ruleBookRunnerIsThreadSafe() throws TimeoutException {
    final Waiter waiter = new Waiter();

    RuleBook ruleBook = new RuleBookRunner("com.deliveredtechnologies.rulebook.runner");

    FactMap<String> equalFacts1 = new FactMap<>();
    equalFacts1.setValue("fact1", "Fact");
    equalFacts1.setValue("fact2", "Fact");

    FactMap<String> equalFacts2 = new FactMap<>();
    equalFacts2.setValue("fact1", "Factoid");
    equalFacts2.setValue("fact2", "Factoid");

    FactMap<String> unequalFacts1 = new FactMap<>();
    unequalFacts1.setValue("fact1", "Fact");
    unequalFacts1.setValue("fact2", "Factoid");

    FactMap<String> unequalFacts2 = new FactMap<>();
    unequalFacts2.setValue("fact1", "Some");
    unequalFacts2.setValue("fact2", "Value");

    ExecutorService service = null;

    try {

      service = Executors.newCachedThreadPool();

      service.execute(() -> {
        ruleBook.run(equalFacts1);
        waiter.assertEquals("So Factual Too!", equalFacts1.getValue("fact1"));
        waiter.resume();
        waiter.assertEquals("So Factual!", equalFacts1.getValue("fact2"));
        waiter.resume();
        waiter.assertEquals("Equivalence, Bitches!", ruleBook.getResult().get().toString());
        waiter.resume();
      });

      service.execute(() -> {
        ruleBook.run(unequalFacts2);
        waiter.assertEquals("Some", unequalFacts2.getValue("fact1"));
        waiter.resume();
        waiter.assertEquals("Value", unequalFacts2.getValue("fact2"));
        waiter.resume();
      });

      service.execute(() -> {
        ruleBook.run(equalFacts2);
        waiter.assertEquals("So Factual Too!", equalFacts2.getValue("fact1"));
        waiter.resume();
        waiter.assertEquals("So Factual!", equalFacts2.getValue("fact2"));
        waiter.resume();
        waiter.assertEquals("Equivalence, Bitches!", ruleBook.getResult().get().toString());
        waiter.resume();
      });

      service.execute(() -> {
        ruleBook.run(unequalFacts1);
        waiter.assertEquals("Fact", unequalFacts1.getValue("fact1"));
        waiter.resume();
        waiter.assertEquals("Factoid", unequalFacts1.getValue("fact2"));
        waiter.resume();
      });

      waiter.await();
    } finally {
      if (service != null) {
        service.shutdown();
      }
    }
  }
}
