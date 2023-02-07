/*
       Copyright 2020-2023 IBM Corp All Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ibm.hybrid.cloud.sample.stocktrader.broker;

import com.ibm.hybrid.cloud.sample.stocktrader.broker.client.AccountClient;
import com.ibm.hybrid.cloud.sample.stocktrader.broker.client.CashAccountClient;
import com.ibm.hybrid.cloud.sample.stocktrader.broker.client.PortfolioClient;
import com.ibm.hybrid.cloud.sample.stocktrader.broker.client.TradeHistoryClient;
import com.ibm.hybrid.cloud.sample.stocktrader.broker.json.Account;
import com.ibm.hybrid.cloud.sample.stocktrader.broker.json.Broker;
import com.ibm.hybrid.cloud.sample.stocktrader.broker.json.CashAccount;
import com.ibm.hybrid.cloud.sample.stocktrader.broker.json.Feedback;
import com.ibm.hybrid.cloud.sample.stocktrader.broker.json.Portfolio;
import com.ibm.hybrid.cloud.sample.stocktrader.broker.json.WatsonInput;


import java.io.PrintWriter;
import java.io.StringWriter;

//Logging (JSR 47)
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

//CDI 2.0
import javax.inject.Inject;
import javax.enterprise.context.RequestScoped;

//mpConfig 1.3
import org.eclipse.microprofile.config.inject.ConfigProperty;

//mpJWT 1.1
import org.eclipse.microprofile.auth.LoginConfig;

//mpRestClient 1.3
import org.eclipse.microprofile.rest.client.inject.RestClient;

//Servlet 4.0
import javax.servlet.http.HttpServletRequest;

//JAX-RS 2.1 (JSR 339)
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Path;

@ApplicationPath("/")
@Path("/")
@LoginConfig(authMethod = "MP-JWT", realmName = "jwt-jaspi")
@RequestScoped //enable interceptors like @Transactional (note you need a WEB-INF/beans.xml in your war)
/** This microservice is the controller in a model-view-controller architecture, doing the routing and
 *  combination of results from other microservices.  Note that the Portfolio microservice it calls is
 *  mandatory, whereas the Account and TradeHistory microservices are optional.
 */
public class BrokerService extends Application {
	private static Logger logger = Logger.getLogger(BrokerService.class.getName());

	private static final double DONT_RECALCULATE = -1.0;

	private static boolean useAccount = false;
	private static boolean useCashAccount = false;
	private static boolean useTradeHistory = false;
	private static boolean useCQRS = false;
	private static boolean initialized = false;
	private static boolean staticInitialized = false;

	private @Inject @ConfigProperty(name = "TEST_MODE", defaultValue = "false") boolean testMode;
	private @Inject @RestClient PortfolioClient portfolioClient;
	private @Inject @RestClient AccountClient accountClient;
	private @Inject @RestClient CashAccountClient cashAccountClient;
	private @Inject @RestClient TradeHistoryClient tradeHistoryClient;

	// Override ODM Client URL if secret is configured to provide URL
	static {
		useAccount = Boolean.parseBoolean(System.getenv("ACCOUNT_ENABLED"));
		logger.info("Account microservice enabled: " + useAccount);

		useCashAccount = Boolean.parseBoolean(System.getenv("CASH_ACCOUNT_ENABLED"));
		logger.info("Cash Account microservice enabled: " + useCashAccount);

		useTradeHistory = Boolean.parseBoolean(System.getenv("TRADE_HISTORY_ENABLED"));
		logger.info("Trade History microservice enabled: " + useTradeHistory);

		useCQRS = Boolean.parseBoolean(System.getenv("CQRS_ENABLED"));
		logger.info("CQRS enabled: " + useCQRS);
		if (useCQRS) logger.warning("CQRS requested, but not using the broker-query flavor of this microservice!");

		String mpUrlPropName = PortfolioClient.class.getName() + "/mp-rest/url";
		String urlFromEnv = System.getenv("PORTFOLIO_URL");
		if ((urlFromEnv != null) && !urlFromEnv.isEmpty()) {
			logger.info("Using Portfolio URL from config map: " + urlFromEnv);
			System.setProperty(mpUrlPropName, urlFromEnv);
		} else {
			logger.info("Portfolio URL not found from env var from config map, so defaulting to value in jvm.options: " + System.getProperty(mpUrlPropName));
		}

		mpUrlPropName = AccountClient.class.getName() + "/mp-rest/url";
		urlFromEnv = System.getenv("ACCOUNT_URL");
		if ((urlFromEnv != null) && !urlFromEnv.isEmpty()) {
			logger.info("Using Account URL from config map: " + urlFromEnv);
			System.setProperty(mpUrlPropName, urlFromEnv);
		} else {
			logger.info("Account URL not found from env var from config map, so defaulting to value in jvm.options: " + System.getProperty(mpUrlPropName));
		}

		mpUrlPropName = CashAccountClient.class.getName() + "/mp-rest/url";
		urlFromEnv = System.getenv("CASH_ACCOUNT_URL");
		if ((urlFromEnv != null) && !urlFromEnv.isEmpty()) {
			logger.info("Using Cash Account URL from config map: " + urlFromEnv);
			System.setProperty(mpUrlPropName, urlFromEnv);
		} else {
			logger.info("Cash Account URL not found from env var from config map, so defaulting to value in jvm.options: " + System.getProperty(mpUrlPropName));
		}

		mpUrlPropName = TradeHistoryClient.class.getName() + "/mp-rest/url";
		urlFromEnv = System.getenv("TRADE_HISTORY_URL");
		if ((urlFromEnv != null) && !urlFromEnv.isEmpty()) {
			logger.info("Using Trade History URL from config map: " + urlFromEnv);
			System.setProperty(mpUrlPropName, urlFromEnv);
		} else {
			logger.info("Trade History URL not found from env var from config map, so defaulting to value in jvm.options: " + System.getProperty(mpUrlPropName));
		}
	}

	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
//	@RolesAllowed({"StockTrader", "StockViewer"}) //Couldn't get this to work; had to do it through the web.xml instead :(
	public Broker[] getBrokers(@Context HttpServletRequest request) {
		if (testMode) return getHardcodedBrokers();

		String jwt = request.getHeader("Authorization");

		logger.fine("Calling PortfolioClient.getPortfolios()");
		Portfolio[] portfolios = portfolioClient.getPortfolios(jwt);

		int portfolioCount=0;
		Broker[] brokers = null;
		Set<Broker> brokersSet = new HashSet<>();
		if (portfolios!=null) {
			portfolioCount = portfolios.length;
			int accountCount = 0;

			brokers = new Broker[portfolioCount];
			Account[] accounts = new Account[portfolioCount];

			if (useAccount) try {
				logger.fine("Calling AccountClient.getAccounts()");
				accounts = accountClient.getAccounts(jwt);
				accountCount = accounts.length;

				// Match up the accounts and portfolios
				// TODO: Pagination should reduce the amount of work to do here
				Map<String, Account> mapOfAccounts = Arrays.stream(accounts).collect(Collectors.toMap(Account::get_id, account -> account));
				Set<String> accountIds = Arrays.stream(accounts).map(Account::get_id).collect(Collectors.toSet());

				brokersSet = Arrays.stream(portfolios)
					.parallel()
					.filter(portfolio -> accountIds.contains(portfolio.getAccountID()))
					.map(portfolio -> {
						String ownerId = portfolio.getAccountID();
						// Don't log here, you'll get a NPE if you uncomment the following line
						// logger.finer("Found account corresponding to the portfolio for " + owner);
						return new Broker(portfolio, mapOfAccounts.get(ownerId));
					})
					.collect(Collectors.toSet());

				// Now handle the cases where there is no matching account-portfolio mapping
				brokersSet.addAll(Arrays.stream(portfolios)
					.parallel()
					.filter(Predicate.not(portfolio -> accountIds.contains(portfolio.getAccountID())))
					.map(portfolio -> {
						// Don't log here, you'll get a NPE
						// logger.finer("Did not find account corresponding to the portfolio for " + owner);
						return new Broker(portfolio, null);
					})
					.collect(Collectors.toSet()));
				
				brokers = brokersSet.stream().toArray(Broker[]::new);
			} catch (Throwable t) {
				logException(t);
			} else {
				//just build the Broker array directly from the Portfolio array, since Accout is disabled
				logger.fine("Handling case of Account being disabled");
				for (int index=0; index<portfolioCount; index++) {
					brokers[index] = new Broker(portfolios[index], null);
				}
			}
		}
		
		logger.fine("Returning "+portfolioCount+" brokers");

		return brokers;
	}

	@POST
	@Path("/{owner}")
	@Produces(MediaType.APPLICATION_JSON)
	//	@RolesAllowed({"StockTrader"}) //Couldn't get this to work; had to do it through the web.xml instead :(
	public Broker createBroker(@PathParam("owner") String owner, @Context HttpServletRequest request) {
		Broker broker = null;
		Portfolio portfolio = null;
		String jwt = request.getHeader("Authorization");

		Account account = null;
		String accountID = null;
		if (useAccount) try {
			logger.fine("Calling AccountClient.createAccount()");
			account = accountClient.createAccount(jwt, owner);
			if (account != null) accountID = account.get_id();
		} catch (Throwable t) {
			logException(t);
		}

		logger.fine("Calling PortfolioClient.createPortfolio()");
		portfolio = portfolioClient.createPortfolio(jwt, owner, accountID);

		String answer = "broker";
		if (portfolio != null) {
			broker = new Broker(portfolio, account);
		} else {
			answer = "null";
		}

		CashAccount cashAccount = null;
		if (useCashAccount) try {
			logger.fine("Calling CashAccountClient.createCashAccount()");
			cashAccount = cashAccountClient.createCashAccount(jwt, owner);
			if (cashAccount != null) {
				broker.setCashAccountBalance(cashAccount.getBalance());
				broker.setCashAccountCurrency(cashAccount.getCurrency());
			}
		} catch (Throwable t) {
			logException(t);
		}

		logger.fine("Returning "+answer);

		return broker;
	}

	@GET
	@Path("/{owner}")
	@Produces(MediaType.APPLICATION_JSON)
//	@RolesAllowed({"StockTrader", "StockViewer"}) //Couldn't get this to work; had to do it through the web.xml instead :(
	public Broker getBroker(@PathParam("owner") String owner, @Context HttpServletRequest request) {
		Broker broker = null;
		Portfolio portfolio = null;
		String jwt = request.getHeader("Authorization");

		logger.fine("Calling PortfolioClient.getPortfolio()");
		portfolio = portfolioClient.getPortfolio(jwt, owner, false);

		String answer = "broker";
		if (portfolio!=null) {
			String accountID = portfolio.getAccountID();
			double total = portfolio.getTotal();
			Account account = null;
			if (useAccount) try {
				logger.fine("Calling AccountClient.getAccount()");
				account = accountClient.getAccount(jwt, accountID, total);
				if (account == null) logger.warning("Account not found for "+owner);
			} catch (Throwable t) {
				logException(t);
			}
			broker = new Broker(portfolio, account);

			CashAccount cashAccount = null;
			if (useCashAccount) try {
				logger.fine("Calling CashAccountClient.getCashAccount()");
				cashAccount = cashAccountClient.getCashAccount(jwt, owner);
				if (cashAccount != null) {
					broker.setCashAccountBalance(cashAccount.getBalance());
					broker.setCashAccountCurrency(cashAccount.getCurrency());
				}
			} catch (Throwable t) {
				logException(t);
			}	
		} else {
			answer = "null";
		}
		logger.fine("Returning "+answer);

		return broker;
	}
    
	@GET
	@Path("/{owner}/returns")
	@Produces(MediaType.TEXT_PLAIN)
	public String getPortfolioReturns(@PathParam("owner") String owner, @Context HttpServletRequest request) {
		if (!useTradeHistory) {
			logger.info("getPortfolioReturns called yet TRADE_HISTORY_ENABLED is false!");
			return "Unavailable";
		}

		String jwt = request.getHeader("Authorization");

		logger.fine("Getting portfolio returns");
		String result = "Unknown";
		Portfolio portfolio = portfolioClient.getPortfolio(jwt, owner, true); //throws a 404 exception if not present
		if (portfolio != null) {
			Double portfolioValue = portfolio.getTotal();

			try {
				result = tradeHistoryClient.getReturns(jwt, owner, portfolioValue);
				logger.fine("Got portfolio returns for "+owner);
			} catch (Throwable t) {
				logger.info("Unable to invoke TradeHistory.  This is an optional microservice and the following exception is expected if it is not deployed");
				logException(t);
			}
		} else {
			logger.warning("Portfolio not found to get returns for "+owner);
		}
		return result;
	}

	@PUT
	@Path("/{owner}")
	@Produces(MediaType.APPLICATION_JSON)
//	@RolesAllowed({"StockTrader"}) //Couldn't get this to work; had to do it through the web.xml instead :(
	public Broker updateBroker(@PathParam("owner") String owner, @QueryParam("symbol") String symbol, @QueryParam("shares") int shares, @Context HttpServletRequest request) {
		Broker broker = null;
		Account account = null;
		Portfolio portfolio = null;
		String jwt = request.getHeader("Authorization");

		double commission = 0.0;
		String accountID = null;
		if (useAccount) try {
			logger.fine("Calling PortfolioClient.getPortfolio() to get accountID in updateBroker()");
			portfolio = portfolioClient.getPortfolio(jwt, owner, false); //throws a 404 if it doesn't exist
			accountID = portfolio.getAccountID();

			logger.fine("Calling AccountClient.getAccount() to get commission in updateBroker()");
			account = accountClient.getAccount(jwt, accountID, DONT_RECALCULATE);
			commission = account.getNextCommission();
		} catch (Throwable t) {
			logException(t);
		}

		logger.fine("Calling PortfolioClient.updatePortfolio()");
		portfolio = portfolioClient.updatePortfolio(jwt, owner, symbol, shares, commission);

		String answer = "broker";
		if (portfolio!=null) {
			double total = portfolio.getTotal();
			account = null;
			if (useAccount) try {
				logger.fine("Calling AccountClient.updateAccount()");
				account = accountClient.updateAccount(jwt, accountID, total);
			} catch (Throwable t) {
				logException(t);
			}
			broker = new Broker(portfolio, account);
		} else {
			answer = "null";
		}
		logger.fine("Returning "+answer);

		return broker;
	}

	@DELETE
	@Path("/{owner}")
	@Produces(MediaType.APPLICATION_JSON)
//	@RolesAllowed({"StockTrader"}) //Couldn't get this to work; had to do it through the web.xml instead :(
	public Broker deleteBroker(@PathParam("owner") String owner, @Context HttpServletRequest request) {
		Broker broker = null;
		Portfolio portfolio = null;
		String jwt = request.getHeader("Authorization");

		logger.fine("Calling PortfolioClient.deletePortfolio()");
		portfolio = portfolioClient.deletePortfolio(jwt, owner);

		String answer = "broker";
		if (portfolio!=null) {
			Account account = null;
			if (useAccount) try {
				String accountID = portfolio.getAccountID();
				logger.fine("Calling AccountClient.deleteAccount()");
				account = accountClient.deleteAccount(jwt, accountID);
			} catch (Throwable t) {
				logException(t);
			}
			broker = new Broker(portfolio, account);

			CashAccount cashAccount = null;
			if (useCashAccount) try {
				logger.fine("Calling CashAccountClient.deleteCashAccount()");
				cashAccount = cashAccountClient.deleteCashAccount(jwt, owner);
				if (cashAccount != null) {
					broker.setCashAccountBalance(cashAccount.getBalance());
					broker.setCashAccountCurrency(cashAccount.getCurrency());
				}
			} catch (Throwable t) {
				logException(t);
			}	
		} else {
			answer = "null";
		}
		logger.fine("Returning "+answer);

		return broker; //maybe this method should return void instead?
	}

	@POST
	@Path("/{owner}/feedback")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
//	@RolesAllowed({"StockTrader"}) //Couldn't get this to work; had to do it through the web.xml instead :(
	public Feedback submitFeedback(@PathParam("owner") String owner, WatsonInput input, @Context HttpServletRequest request) {
		Feedback feedback = null;
		String jwt = request.getHeader("Authorization");

		logger.fine("Calling AccountClient.submitFeedback()");
		feedback = accountClient.submitFeedback(jwt, owner, input);

		String answer = "feedback";
		if (feedback==null) answer = "null";
		logger.fine("Returning "+answer);

		return feedback;
	}

	Broker[] getHardcodedBrokers() {
		Broker john = new Broker("John");
		john.setTotal(1234.56);
		john.setLoyalty("Basic");
		Broker karri = new Broker("Karri");
		karri.setTotal(12345.67);
		karri.setLoyalty("Bronze");
		Broker ryan = new Broker("Ryan");
		ryan.setTotal(23456.78);
		ryan.setLoyalty("Bronze");
		Broker raunak = new Broker("Raunak");
		raunak.setTotal(98765.43);
		raunak.setLoyalty("Silver");
		Broker greg = new Broker("Greg");
		greg.setTotal(123456.78);
		greg.setLoyalty("Gold");
		Broker eric = new Broker("Eric");
		eric.setTotal(1234567.89);
		eric.setLoyalty("Platinum");
		Broker[] brokers = { john, karri, ryan, raunak, greg, eric };
		return brokers;
	}

	static void logException(Throwable t) {
		logger.warning(t.getClass().getName()+": "+t.getMessage());

		//only log the stack trace if the level has been set to at least INFO
		if (logger.isLoggable(Level.INFO)) {
			StringWriter writer = new StringWriter();
			t.printStackTrace(new PrintWriter(writer));
			logger.info(writer.toString());
		}
	}
}
