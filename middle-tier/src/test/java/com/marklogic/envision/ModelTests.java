package com.marklogic.envision;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.client.DatabaseClient;
import com.marklogic.envision.dataServices.EntityModeller;
import com.marklogic.envision.model.ModelService;
import com.marklogic.envision.session.SessionManager;
import com.marklogic.grove.boot.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.file.Path;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = Application.class)
public class ModelTests extends BaseTest {

	@Autowired
	ModelService modelService;

	@Autowired
	SessionManager sessionManager;

	@BeforeEach
	void setUp() throws Exception {
		removeUser(ACCOUNT_NAME);

		clearStagingFinalAndJobDatabases();
		installEnvisionModules();

		registerAccount();
		sessionManager.setHubClient(ACCOUNT_NAME, getAdminHubClient());
	}

	@Test
	@WithMockUser(username = ACCOUNT_NAME)
	public void toDatahub() throws Exception {
		Path modelsDir = projectPath.resolve("models");
		modelsDir.toFile().mkdirs();
		modelService.setModelsDir(modelsDir.toFile());
		modelService.saveModel(getNonAdminHubClient(), getResourceStream("models/model.json"));
		DatabaseClient client = getNonAdminHubClient().getFinalClient();
		JsonNode result = EntityModeller.on(client).toDatahub();
		jsonAssertEquals(getResource("output/esEntities.json"), result);
	}

	@Test
	public void fromDatahub() throws Exception {
		CustomComparator resultCompare = new CustomComparator(JSONCompareMode.STRICT,
			new Customization("nodes.*.properties[*]._propId", (o1, o2) -> true)
		);

		installFinalDoc("esEntities/Department.entity.json", "/entities/Department.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/Employee.entity.json", "/entities/Employee.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/MegaCorp.entity.json", "/entities/MegaCorp.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/Organization.entity.json", "/entities/Organization.entity.json", "http://marklogic.com/entity-services/models");
		installFinalDoc("esEntities/Planet.entity.json", "/entities/Planet.entity.json", "http://marklogic.com/entity-services/models");

		DatabaseClient client = getFinalClient();
		JsonNode result = EntityModeller.on(client).fromDatahub();
		jsonAssertEquals(getResource("output/esModel.json"), result, resultCompare);
	}
}
