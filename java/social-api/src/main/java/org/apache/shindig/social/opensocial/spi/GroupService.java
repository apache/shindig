package org.apache.shindig.social.opensocial.spi;

import java.util.Set;
import java.util.concurrent.Future;

import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.protocol.RestfulCollection;
import org.apache.shindig.social.opensocial.model.Group;

public interface GroupService {

	public Future<RestfulCollection<Group>> getGroups(UserId userId, CollectionOptions options, Set<String> fields, SecurityToken token);

}
