import axios from 'axios';

const API = axios.create({
    baseURL: 'http://localhost:8080/',
});

export const executeCode = async (language, sourceCode) => {
    console.log("sourceCode:", sourceCode);

    const response = await API.post("/store-text", {
        text: sourceCode,
      });
    return response.data;
};