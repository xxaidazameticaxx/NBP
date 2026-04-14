package ba.unsa.etf.NBP;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"JWT_SECRET=12345678901234567890123456789012"
})
class NbpApplicationTests {

	@Test
	void contextLoads() {
	}

}
