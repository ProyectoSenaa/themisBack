package co.sena.edu.themis.Resolver.DgsComponent.DgsFetcher;

import co.sena.edu.themis.Business.FollowUpBusiness;
import co.sena.edu.themis.Entity.FollowUp;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsEntityFetcher;
import org.modelmapper.ModelMapper;

import java.util.Map;

@DgsComponent
public class FollowUpFetcher {
    private final ModelMapper modelMapper = new ModelMapper();
    private final FollowUpBusiness followUpBusiness;

    public FollowUpFetcher(FollowUpBusiness followUpBusiness) {
        this.followUpBusiness = followUpBusiness;
    }

    @DgsEntityFetcher(name = "FollowUp")
    public FollowUp getFollowUp(Map<String, Object> values) {
        System.out.println("fetcher followup");
        Object idObj = values.get("id");
        Long id = null;
        try {
            if (idObj instanceof String s) {
                id = s.isEmpty() ? null : Long.parseLong(s);
            } else if (idObj instanceof Number n) {
                id = n.longValue();
            }
        } catch (Exception ignored) {
            // leave id as null
        }
        if (id == null) {
            return null;
        }
        return modelMapper.map(followUpBusiness.followUpById(id), FollowUp.class);
    }
}

