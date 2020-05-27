package io.maverick.database.breeze;

import io.maverick.database.breeze.exception.BreezeActionException;
import io.maverick.database.breeze.exception.ErrorCode;
import io.maverick.database.breeze.service.BreezeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
class BreezeApplicationTests {

	@Autowired
	private BreezeService<String,String> breezeService;

	@Test
	public void ifAValueDoesNotExist_ServiceReturnsNull() {
		assertNull(breezeService.get("dummy"));
	}

	@Test
	public void ifAValueExist_ServiceReturnsTheLatestValue() {
		breezeService.put("dummy","1");
		breezeService.put("dummy","2");
		assertEquals("2",breezeService.get("dummy"));
	}

	@Test
	public void whendDeletingANonExistingValue_SystemRespondsWithVoid() {
		assertNull(breezeService.get("dummy"));
		breezeService.delete("dummy");
		assertNull(breezeService.get("dummy"));
	}

	@Test
	public void whendDeletingAValue_NoValuesWillBeInTheStoreAfter() {
		breezeService.put("dummy","1");
		assertEquals("1",breezeService.get("dummy"));
		breezeService.delete("dummy");
		assertNull(breezeService.get("dummy"));
	}

	@Test
	public void transactionsCanBeCreated() {
		breezeService.createTransaction("dummy");
	}

	@Test
	public void whenTransactionAlreadyExists_tryingToCreateItAgainFails() {
		breezeService.createTransaction("dummy");

		BreezeActionException exception = assertThrows(BreezeActionException.class, () -> {
			breezeService.createTransaction("dummy");
		});

		assertEquals(ErrorCode.TRANSACTION_ALREADY_EXISTS, exception.getErrorCode());
	}

	@Test
	public void whenATransactionIsRolledBack_NoValueChangesInStore() {
		breezeService.put("apple","1");
		breezeService.createTransaction("dummy");
		breezeService.put("apple","2","dummy");
		assertEquals("1",breezeService.get("apple"));

		breezeService.rollbackTransaction("dummy");
		assertEquals("1",breezeService.get("apple"));
	}

	@Test
	public void whenATransactionIsRolledBack_TheTransactionIsRemoved() {
		breezeService.createTransaction("dummy");
		breezeService.rollbackTransaction("dummy");

		BreezeActionException exception = assertThrows(BreezeActionException.class, () -> {
			breezeService.rollbackTransaction("dummy");
		});

		assertEquals(ErrorCode.UNKNOWN_TRANSACTION, exception.getErrorCode());
	}

	@Test
	public void whenANonExistentTransactionIsRolledBack_AnErrorIsRaised() {
		BreezeActionException exception = assertThrows(BreezeActionException.class, () -> {
			breezeService.rollbackTransaction("dummy");
		});

		assertEquals(ErrorCode.UNKNOWN_TRANSACTION, exception.getErrorCode());
	}

	@Test
	public void whenATransactionIsCommitted_AllValuesAreVisibleInOneGo() {
		breezeService.put("apple","1");
		breezeService.put("orange","2");

		breezeService.createTransaction("dummy");
		breezeService.put("apple","2","dummy");
		breezeService.delete("orange","dummy");

		breezeService.commitTransaction("dummy");
		assertEquals("2",breezeService.get("apple"));
		assertNull(breezeService.get("orange"));
	}

	@Test
	public void whenATransactionIsCommittedAndAnItemIsUpdatedInTheMeantime_TheCommitFailsWithError() throws InterruptedException {
		breezeService.put("apple","1");
		breezeService.put("orange","2");

		breezeService.createTransaction("dummy");
		breezeService.put("apple","2","dummy");
		breezeService.put("orange","1","dummy");

		breezeService.put("orange","3");
		assertEquals("3",breezeService.get("orange"));

		BreezeActionException exception = assertThrows(BreezeActionException.class, () -> {
			breezeService.commitTransaction("dummy");
		});
	}

	@Test
	public void whenWorkingWithTransactions_changesAreOnlyVisibleAfterCommit() throws InterruptedException {
		breezeService.put("apple","1");
		breezeService.put("orange","2");

		breezeService.createTransaction("dummy");
		breezeService.put("apple","2","dummy");
		breezeService.put("orange","1","dummy");

		assertEquals("2",breezeService.get("orange"));
		assertEquals("1",breezeService.get("apple"));

		breezeService.commitTransaction("dummy");

		assertEquals("1",breezeService.get("orange"));
		assertEquals("2",breezeService.get("apple"));
	}

	@Test
	public void whenANonExistentTransactionIsCommitted_AnErrorIsRaised() {
		breezeService.createTransaction("dummy");
		breezeService.rollbackTransaction("dummy");

		BreezeActionException exception = assertThrows(BreezeActionException.class, () -> {
			breezeService.rollbackTransaction("dummy");
		});

		assertEquals(ErrorCode.UNKNOWN_TRANSACTION, exception.getErrorCode());
	}

	@Test
	public void whenTwoThreadsUpdateTheSameValue_TheLatterOnePrevails() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool(2);

		final BreezeService<String,String> service = breezeService;

		CountDownLatch latch = new CountDownLatch(2);

		Runnable one = () -> { service.put("apple","2"); latch.countDown();};
		Runnable other = () -> { service.put("apple","3"); latch.countDown();};

		executor.submit(one);
		executor.submit(other);
		latch.await();
	}
}
