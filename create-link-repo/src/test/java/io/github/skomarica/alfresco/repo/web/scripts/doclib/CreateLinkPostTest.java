package io.github.skomarica.alfresco.repo.web.scripts.doclib;

import static org.junit.Assert.assertNotNull;

import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.tradeshift.test.remote.Remote;
import com.tradeshift.test.remote.RemoteTestRunner;

@RunWith(RemoteTestRunner.class)
@Remote(runnerClass = SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:alfresco/application-context.xml")
public class CreateLinkPostTest
{
	private static Log logger = LogFactory.getLog(CreateLinkPostTest.class);

	@Autowired
	@Qualifier("NodeService")
	protected NodeService nodeService;

	@Test
	public void testWiring()
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Executing testWiring()...");
		}

		assertNotNull(nodeService);

		if (logger.isDebugEnabled())
		{
			logger.debug("testWiring() successfully executed.");
		}
	}

}