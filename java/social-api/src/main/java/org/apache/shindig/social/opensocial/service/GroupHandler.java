package org.apache.shindig.social.opensocial.service;

import java.util.Set;
import java.util.concurrent.Future;

import org.apache.shindig.protocol.HandlerPreconditions;
import org.apache.shindig.protocol.Operation;
import org.apache.shindig.protocol.ProtocolException;
import org.apache.shindig.protocol.Service;
import org.apache.shindig.social.opensocial.spi.CollectionOptions;
import org.apache.shindig.social.opensocial.spi.GroupService;
import org.apache.shindig.social.opensocial.spi.UserId;

import com.google.inject.Inject;


@Service(name = "groups", path="/{userId}")
public class GroupHandler {

	private final GroupService service;

	 @Inject
	 public GroupHandler(GroupService service){
		 this.service = service;
	 }

	 @Operation(httpMethods="GET")
	  public Future<?> get(SocialRequestItem request) throws ProtocolException {
		 Set<UserId> userIds = request.getUsers();
		 CollectionOptions options = new CollectionOptions(request);

		 // Preconditions
		 HandlerPreconditions.requireNotEmpty(userIds, "No userId specified");
		 HandlerPreconditions.requireSingular(userIds, "Only one userId must be specified");

		 return service.getGroups(userIds.iterator().next(), options, request.getFields(), request.getToken());

	 }

}
