create or replace function enum(p_value text) returns boolean immutable cost 1 language plpgsql as $$
  begin
    if p_value is null then
      return true;
    else
      if lower(trim(p_value)) = p_value and p_value != '' then
        return true;
      else
        return false;
      end if;
    end if;
  end
$$;
