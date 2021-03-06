package edu.sibinfo.spring.web.module03.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import edu.sibinfo.spring.web.module03.dao.ClientDao;
import edu.sibinfo.spring.web.module03.dao.PhoneType;
import edu.sibinfo.spring.web.module03.domain.Client;
import edu.sibinfo.spring.web.module03.domain.Phone;
import edu.sibinfo.spring.web.module03.dto.ClientDTO;
import edu.sibinfo.spring.web.module03.dto.PhoneDTO;
import edu.sibinfo.spring.web.module03.service.ClientRegisteredEvent;
import edu.sibinfo.spring.web.module03.service.ClientService;

@Service
public class ClientServiceImpl implements ClientService {

	private final ClientDao clientDao;
	private final MessageDigest encoder;
	private final ApplicationEventPublisher publisher;
	private final ConversionService conversionService;  

	@Autowired
	public ClientServiceImpl(ClientDao clientDao, MessageDigest encoder, ConversionService conversionService, ApplicationEventPublisher publisher) {
		super();
		this.clientDao = clientDao;
		this.encoder = encoder;
		this.publisher = publisher;
		this.conversionService = conversionService;
	}

	@Override
	public ClientDTO register(String firstName, String familyName, String mobile) {
		Client client = new Client(familyName, firstName);
		Phone phone = new Phone(mobile, PhoneType.MOBILE);
		client.addPhone(phone);
		clientDao.save(client);
		ClientDTO result = conversionService.convert(client, ClientDTO.class);
		publisher.publishEvent(new ClientRegisteredEvent(result, createDTO(phone)));
		return result;
	}
	
	@Override
	@Transactional
	public void addPhone(ClientDTO clientDTO, String number, PhoneType phoneType) {
		Phone phone = new Phone(number, phoneType);
		Client client = clientDao.findOne(clientDTO.getId());
		client.addPhone(phone);
		clientDao.save(client);
	}

	@Override
	@Transactional
	public void setPassword(ClientDTO clientDTO, String password) {
		Client client = clientDao.findOne(clientDTO.getId());
		client.setPasswordEncoded(encoder.digest(password.getBytes(StandardCharsets.UTF_8)));
		clientDao.save(client);		
	}

	@Override
	public void deleteClient(ClientDTO client) {
		clientDao.delete(client.getId()); // clientDao.delete(client) fails in non-transactional context 
	}

	@Override
	public ClientDTO findByPhone(String number) {
		return conversionService.convert(clientDao.findByPhone(number), ClientDTO.class);
	}

	@Override
	public ClientDTO findByFamilyName(String familyName) {
		return conversionService.convert(clientDao.findByFamilyName(familyName), ClientDTO.class);
	}

	@Transactional
	@Override
	public ClientDTO findByFamilyName(String familyName, Consumer<Client> consumer) {
		Client c = clientDao.findByFamilyName(familyName);
		consumer.accept(c);
		return conversionService.convert(c, ClientDTO.class);
	}

	@Override
	public Iterable<ClientDTO> search(String charactersitics) {
		Iterable<Client> clients = clientDao.search(charactersitics);
		return createDtoList(clients);
	}

	private List<ClientDTO> createDtoList(Iterable<Client> clients) {
		List<ClientDTO> result = new ArrayList<>(); 
		for (Client c : clients) {
			result.add(conversionService.convert(c, ClientDTO.class));
		}
		return result;
	}

	@SuppressWarnings("serial")
	@Override
	public Page<ClientDTO> findAll(int page) {
		Page<Client> domainPage = clientDao.findAll(new PageRequest(page, 10, new Sort("familyName", "firstName")));
		final int pageCount = domainPage.getTotalPages(); 
		return new PageImpl<ClientDTO>(createDtoList(domainPage)) {
			@Override
			public int getTotalPages() {
				return pageCount;
			}
		};
	}

	@Override
	public ClientDTO findOne(long clientId) {
		return conversionService.convert(clientDao.findOne(clientId), ClientDTO.class);
	}

	private PhoneDTO createDTO(Phone phone) {
		return new PhoneDTO(phone.getNumber(), phone.getPhoneType().name());
	}

	@Override
	@Transactional
	public void update(ClientDTO dto) {
		Client client = clientDao.findOne(dto.getId());
		client.setFamilyName(dto.getLastName());
		client.setFirstName(dto.getName());
	}
}
