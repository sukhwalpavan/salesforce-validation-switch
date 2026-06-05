import { useEffect, useState } from "react";
import axios from "axios";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

function App() {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(false);

  const loginWithSalesforce = () => {
    window.location.href = `${API_URL}/auth/login`;
  };

  const getValidationRules = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`${API_URL}/api/validation-rules`);
      setRules(response.data);
    } catch (error) {
      alert("Error while fetching validation rules");
      console.log(error);
    } finally {
      setLoading(false);
    }
  };

  const toggleRule = async (rule) => {
    try {
      const newStatus = !rule.active;

      await axios.patch(`${API_URL}/api/validation-rules/${rule.id}`, {
        active: newStatus,
      });

      setRules(
        rules.map((item) =>
          item.id === rule.id ? { ...item, active: newStatus } : item
        )
      );

      alert("Validation rule updated successfully");
    } catch (error) {
      alert("Error while updating validation rule");
      console.log(error);
    }
  };

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);

    if (params.get("login") === "success") {
      alert("Salesforce login successful");
    }
  }, []);

  return (
    <div style={{ padding: "30px", fontFamily: "Arial" }}>
      <h1>Salesforce Validation Rule Switch</h1>

      <button onClick={loginWithSalesforce}>Login with Salesforce</button>

      <br />
      <br />

      <button onClick={getValidationRules}>
        {loading ? "Loading..." : "Get Validation Rules"}
      </button>

      <br />
      <br />

      <table border="1" cellPadding="10" width="100%">
        <thead>
          <tr>
            <th>Rule Name</th>
            <th>Object</th>
            <th>Status</th>
            <th>Action</th>
          </tr>
        </thead>

        <tbody>
          {rules.map((rule) => (
            <tr key={rule.id}>
              <td>{rule.validationName}</td>
              <td>{rule.objectName}</td>
              <td>{rule.active ? "Active" : "Inactive"}</td>
              <td>
                <button onClick={() => toggleRule(rule)}>
                  {rule.active ? "Deactivate" : "Activate"}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {rules.length === 0 && <p>No validation rules loaded.</p>}
    </div>
  );
}

export default App;