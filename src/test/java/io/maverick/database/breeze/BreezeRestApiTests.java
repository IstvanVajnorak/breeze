package io.maverick.database.breeze;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.maverick.database.breeze.domain.ValueDTO;
import io.maverick.database.breeze.exception.BreezeActionException;
import io.maverick.database.breeze.exception.ErrorCode;
import io.maverick.database.breeze.service.BreezeService;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@SpringBootTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@WebAppConfiguration
class BreezeRestApiTests {

	private MockMvc mockMvc;

	@Spy
	private BreezeService<String,String> breezeService;

	@InjectMocks
	private BreezeController breezeControllerMock;

	@BeforeEach
	public void startService() throws Exception{
		MockitoAnnotations.initMocks(this);
		mockMvc = MockMvcBuilders.standaloneSetup(breezeControllerMock).build();
	}

	@Test
	public void ifAValueDoesNotExist_ServiceReturnsNull() throws Exception {

		when(breezeService.get("apple")).thenReturn(null);

		mockMvc.perform(get("/entry/apple"))
		.andExpect(status().isOk())
		.andExpect(content().contentType(MediaType.APPLICATION_JSON))
		.andDo(MockMvcResultHandlers.print())
		.andExpect(jsonPath("$.key").value(is("apple")))
		.andExpect(jsonPath("$.value").value(IsNull.nullValue()));
	}

	@Test
	public void ifAValueDoesNotExist_ServiceReturnsAValue() throws Exception {

		when(breezeService.get("apple")).thenReturn("3");

		mockMvc.perform(get("/entry/apple"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$.key").value(is("apple")))
				.andExpect(jsonPath("$.value").value("3"));
	}

	@Test
	public void whenDeletingAnEntry_AProperResponseComesBack() throws Exception {

		doNothing().when(breezeService).delete("apple");

		mockMvc.perform(delete("/entry/apple"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$").value(is("DELETED")));
	}

	@Test
	public void whenUpsertingAnEntry_AProperResponseComesBack() throws Exception {

		doNothing().when(breezeService).put("apple","3");

		//creating the request object
		final ValueDTO<String,String> value = new ValueDTO<>("","3");
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
		ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
		String requestJson=ow.writeValueAsString(value);

		mockMvc.perform(post("/entry/apple").contentType(MediaType.APPLICATION_JSON).content(requestJson))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$").value(is("UPSERTED")));
	}

	@Test
	public void whenCreatingANonExistentTransaction_AProperResponseComesBack() throws Exception {

		doNothing().when(breezeService).createTransaction("dummy");

		mockMvc.perform(post("/transaction/dummy"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$").value(is("CREATED")));
	}

	@Test
	public void whenTryingToCreateAnAlreadyExistingTransaction_AnErrorIsRenderedCorrectly() throws Exception {
		final String description = "Error description";
		final ErrorCode alreadyExists = ErrorCode.TRANSACTION_ALREADY_EXISTS;

		doThrow(new BreezeActionException(alreadyExists,description)).when(breezeService).createTransaction("dummy");

		mockMvc.perform(post("/transaction/dummy"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$.cause").value(alreadyExists.toString()))
				.andExpect(jsonPath("$.message").value(description))
				.andExpect(jsonPath("$.errorCode").value(alreadyExists.getCode()));
	}

	@Test
	public void whenRollingBackAnExistingTransaction_AProperResponseComesBack() throws Exception {

		doNothing().when(breezeService).rollbackTransaction("dummy");

		mockMvc.perform(post("/transaction/dummy/rollback"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$").value(is("ROLLED BACK")));
	}

	@Test
	public void whenTryingToRollBackANonExistent_AnErrorIsRenderedCorrectly() throws Exception {
		final String description = "Error description";
		final ErrorCode unknownTransaction = ErrorCode.UNKNOWN_TRANSACTION;

		doThrow(new BreezeActionException(unknownTransaction,description)).when(breezeService).createTransaction("dummy");

		mockMvc.perform(post("/transaction/dummy"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$.cause").value(unknownTransaction.toString()))
				.andExpect(jsonPath("$.message").value(description))
				.andExpect(jsonPath("$.errorCode").value(unknownTransaction.getCode()));
	}

	@Test
	public void whenComittingAnExistingTransaction_AProperResponseComesBack() throws Exception {

		doNothing().when(breezeService).commitTransaction("dummy");

		mockMvc.perform(post("/transaction/dummy/commit"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$").value(is("COMMITTED")));
	}

	@Test
	public void whenTryingToCommitANonExistent_AnErrorIsRenderedCorrectly() throws Exception {
		final String description = "Error description";
		final ErrorCode unknownTransaction = ErrorCode.UNKNOWN_TRANSACTION;

		doThrow(new BreezeActionException(unknownTransaction,description)).when(breezeService).createTransaction("dummy");

		mockMvc.perform(post("/transaction/dummy"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$.cause").value(unknownTransaction.toString()))
				.andExpect(jsonPath("$.message").value(description))
				.andExpect(jsonPath("$.errorCode").value(unknownTransaction.getCode()));
	}

	@Test
	public void whenTryingToCommitATransactionWithChangesThatAreNotMostRecent_AnErrorIsRenderedCorrectly() throws Exception {
		final String description = "Error description";
		final ErrorCode uncommitableTransaction = ErrorCode.UNCOMMITABLE_TRANSACTION;

		doThrow(new BreezeActionException(uncommitableTransaction,description)).when(breezeService).createTransaction("dummy");

		mockMvc.perform(post("/transaction/dummy"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$.cause").value(uncommitableTransaction.toString()))
				.andExpect(jsonPath("$.message").value(description))
				.andExpect(jsonPath("$.errorCode").value(uncommitableTransaction.getCode()));
	}

}
