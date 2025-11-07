package com.eazybytes.accounts.service.impl;

import com.eazybytes.accounts.constants.AccountsConstants;
import com.eazybytes.accounts.dto.AccountMsgDto;
import com.eazybytes.accounts.dto.AccountsDto;
import com.eazybytes.accounts.dto.CustomerDto;
import com.eazybytes.accounts.entities.Accounts;
import com.eazybytes.accounts.entities.Customer;
import com.eazybytes.accounts.exception.CustomerAlreadyExistsException;
import com.eazybytes.accounts.exception.ResourceNotFoundException;
import com.eazybytes.accounts.mapper.AccountsMapper;
import com.eazybytes.accounts.mapper.CustomerMapper;
import com.eazybytes.accounts.repository.AccountsRepository;
import com.eazybytes.accounts.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
public class IAccountsService implements com.eazybytes.accounts.service.IAccountsService {

    private final static Logger logger = LoggerFactory.getLogger("IAccountsService.class");
    private AccountsRepository accountsRepository;
    private CustomerRepository customerRepository;
    private StreamBridge streamBridge;

    @Override
    public void createAccount(CustomerDto customerDto) {
        Customer customer = CustomerMapper.mapToCustomer(customerDto, new Customer());
        Optional<Customer> optionalCustomer = customerRepository.findByMobileNumber(customerDto.getMobileNumber());
        if(optionalCustomer.isPresent()){
            throw new CustomerAlreadyExistsException("Customer already exists with mobile number " + customerDto.getMobileNumber());
        }
        Customer savedCustomer = customerRepository.save(customer);
        Accounts savedAccount = accountsRepository.save(createNewAccount(savedCustomer));
        sendCommunication(savedAccount,savedCustomer);
    }

    public void sendCommunication(Accounts account, Customer customer){
        var accountMsgDto = new AccountMsgDto(account.getAccountNumber(), customer.getName(), customer.getEmail(),customer.getMobileNumber());

        logger.info("Sending communication info for the details: {}", accountMsgDto);
        var result = streamBridge.send("sendCommunication-out-0",accountMsgDto);
        logger.info("Is the communication successfully received: {}", result);
    }

    @Override
    public CustomerDto fetchAccount(String mobileNumber) {
        Customer customer= customerRepository.findByMobileNumber(mobileNumber).
                orElseThrow(() -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));

        Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).
                orElseThrow(()-> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString()));

        CustomerDto customerDto= CustomerMapper.mapToCustomerDto(customer,new CustomerDto());
        customerDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));

        return customerDto;
    }

    @Override
    public boolean updateAccount(CustomerDto customerDto) {
        boolean isUpdated = false;
        AccountsDto accountsDto = customerDto.getAccountsDto();
        if(accountsDto != null){
            Accounts account = accountsRepository.findById(accountsDto.getAccountNumber()).
                    orElseThrow(()->new ResourceNotFoundException("Account","Account Number",accountsDto.getAccountNumber().toString()));
            AccountsMapper.mapToAccounts(accountsDto,account);
            accountsRepository.save(account);

            Customer customer = customerRepository.findById(account.getCustomerId()).orElseThrow(()->new ResourceNotFoundException("Customer","CustomerId",account.getCustomerId().toString()));

            CustomerMapper.mapToCustomer(customerDto,customer);
            customerRepository.save(customer);
            isUpdated = true;
        }
        return isUpdated;
    }

    @Override
    public boolean deleteAccount(String mobileNumber) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).
                orElseThrow(()->new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));
        accountsRepository.deleteByCustomerId(customer.getCustomerId());
        customerRepository.deleteById(customer.getCustomerId());
        return true;

    }

    @Override
    public boolean updateCommunicationStatus(Long accountNumber) {
        boolean isUpdated=false;
        if(accountNumber!=null){
            Accounts accounts = accountsRepository.findById(accountNumber).orElseThrow(
                    ()->  new ResourceNotFoundException("Account","Account Number", accountNumber.toString() )
            );
            accounts.setCommunicationSwitch(true);
            accountsRepository.save(accounts);
            isUpdated=true;
        }
        return isUpdated;
    }

    private Accounts createNewAccount(Customer customer) {
        Accounts newAccount = new Accounts();

        newAccount.setCustomerId(customer.getCustomerId());
        Long randomAccessNumber = 1000000000L + new Random().nextInt(900000000);
        newAccount.setAccountNumber(randomAccessNumber);
        newAccount.setBranchAddress(AccountsConstants.ADDRESS);
        newAccount.setAccountType(AccountsConstants.SAVINGS);
        return newAccount;
    }


}
